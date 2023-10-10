/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.branding.preference.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCacheEntry;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCacheKey;
import org.wso2.carbon.identity.branding.preference.resolver.internal.BrandingResolverComponentDataHolder;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CLEARING_BRANDING_PREFERENCE_RESOLVER_CACHE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NAME_SEPARATOR;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleClientException;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;

/**
 * UI Branding Preference Resolver Implementation.
 */
public class UIBrandingPreferenceResolverImpl implements UIBrandingPreferenceResolver {

    private static final Log LOG = LogFactory.getLog(UIBrandingPreferenceResolverImpl.class);

    private final BrandedOrgCache brandedOrgCache;

    /**
     * UI branding preference resolver implementation constructor.
     *
     * @param brandedOrgCache Cache instance
     */
    public UIBrandingPreferenceResolverImpl(BrandedOrgCache brandedOrgCache) {

        this.brandedOrgCache = brandedOrgCache;
    }

    @Override
    public BrandingPreference resolveBranding(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        String organizationId = getOrganizationId();
        String currentTenantDomain = getTenantDomain();

        OrganizationManager organizationManager =
                BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();

        /* Tenant domain will always be carbon.super for SaaS apps (ex. myaccount). Hence need to resolve
          tenant domain from the name parameter. */
        if (ORGANIZATION_TYPE.equals(type) &&
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(currentTenantDomain)) {
            currentTenantDomain = name;
            try {
                organizationId = organizationManager.resolveOrganizationId(currentTenantDomain);
            } catch (OrganizationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, currentTenantDomain);
            }
        }

        if (organizationId != null) {
            BrandedOrgCacheEntry valueFromCache =
                    brandedOrgCache.getValueFromCache(new BrandedOrgCacheKey(organizationId), currentTenantDomain);
            if (valueFromCache != null) {
                Optional<BrandingPreference> brandingPreference =
                        getBrandingPreference(type, name, locale, valueFromCache.getBrandingResolvedTenant());
                return brandingPreference.orElseThrow(
                        () -> handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS, getTenantDomain()));
            }

            // No cache found. Start with current organization.
            Optional<BrandingPreference> brandingPreference =
                    getBrandingPreference(type, name, locale, currentTenantDomain);
            if (brandingPreference.isPresent()) {
                return brandingPreference.get();
            }

            try {
                Organization organization = organizationManager.getOrganization(organizationId, false, false);
                // There's no need to resolve branding preferences for super tenant since it is the root organization.
                if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(currentTenantDomain)) {
                    // Get the details of the parent organization and resolve the branding preferences.
                    String parentId = organization.getParent().getId();
                    String parentTenantDomain = organizationManager.resolveTenantDomain(parentId);
                    int parentDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(parentId);

                    // Get the minimum hierarchy depth that needs to be reached to resolve branding preference
                    int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;

                    while (parentDepthInHierarchy >= minHierarchyDepth) {
                        brandingPreference =
                                getBrandingPreference(type, name, locale, parentTenantDomain);
                        if (brandingPreference.isPresent()) {
                            addToCache(organizationId, currentTenantDomain, parentTenantDomain);
                            return brandingPreference.get();
                        }

                        /*
                            Get ancestor organization ids (including itself) of a given organization. The list is sorted
                            from given organization id to the root organization id.
                         */
                        List<String> ancestorOrganizationIds = organizationManager.getAncestorOrganizationIds(parentId);
                        if (!ancestorOrganizationIds.isEmpty() && ancestorOrganizationIds.size() > 1) {
                            // Go to the parent organization again.
                            parentId = ancestorOrganizationIds.get(1);
                            parentTenantDomain = organizationManager.resolveTenantDomain(parentId);
                            parentDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(parentId);
                        } else {
                            // Reached to the root of the organization tree.
                            parentDepthInHierarchy = -1;
                        }
                    }
                }
            } catch (OrganizationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, getTenantDomain());
            }

            // No branding found. Adding the same tenant domain to cache to avoid the resolving in the next run.
            addToCache(organizationId, currentTenantDomain, currentTenantDomain);
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS, getTenantDomain());
        } else {
            // No need to resolve the branding preference. Try to fetch the config from the same org.
            Optional<BrandingPreference> brandingPreference =
                    getBrandingPreference(type, name, locale, currentTenantDomain);
            return brandingPreference.orElseThrow(
                    () -> handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS, getTenantDomain()));
        }
    }

    @Override
    public void clearBrandingResolverCache(String currentTenantDomain) throws BrandingPreferenceMgtException {
        String organizationId = getOrganizationId();
        if (organizationId == null) {
            try {
                // If organization id is not available in the context, try to resolve it from tenant domain
                OrganizationManager organizationManager =
                        BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();
                organizationId = organizationManager.resolveOrganizationId(currentTenantDomain);
            } catch (OrganizationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_CLEARING_BRANDING_PREFERENCE_RESOLVER_CACHE,
                        currentTenantDomain);
            }
        }

        BrandedOrgCacheKey brandedOrgCacheKey = new BrandedOrgCacheKey(organizationId);
        BrandedOrgCacheEntry valueFromCache =
                brandedOrgCache.getValueFromCache(brandedOrgCacheKey, currentTenantDomain);
        if (valueFromCache != null) {
            brandedOrgCache.clearCacheEntry(brandedOrgCacheKey, currentTenantDomain);
        }
    }

    private void addToCache(String brandedOrgId, String brandedTenantDomain, String brandingInheritedTenantDomain) {

        BrandedOrgCacheKey cacheKey = new BrandedOrgCacheKey(brandedOrgId);
        BrandedOrgCacheEntry cacheEntry = new BrandedOrgCacheEntry(brandingInheritedTenantDomain);
        brandedOrgCache.addToCache(cacheKey, cacheEntry, brandedTenantDomain);
    }

    private Optional<BrandingPreference> getBrandingPreference(String type, String name, String locale,
                                                               String tenantDomain)
            throws BrandingPreferenceMgtException {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

            String resourceName = getResourceName(type, name, locale);
            List<ResourceFile> resourceFiles = getConfigurationManager().getFiles(BRANDING_RESOURCE_TYPE, resourceName);
            if (resourceFiles.isEmpty()) {
                return Optional.empty();
            }
            if (StringUtils.isBlank(resourceFiles.get(0).getId())) {
                return Optional.empty();
            }

            InputStream inputStream = getConfigurationManager().getFileById
                    (BRANDING_RESOURCE_TYPE, resourceName, resourceFiles.get(0).getId());
            if (inputStream == null) {
                return Optional.empty();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Branding preference for tenant: " + tenantDomain + " is retrieved successfully.");
            }
            return Optional.of(buildBrandingPreferenceFromResource(inputStream, type, name, locale));
        } catch (ConfigurationManagementException e) {
            if (!RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, tenantDomain, e);
            }
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE, tenantDomain);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return Optional.empty();
    }

    private ConfigurationManager getConfigurationManager() {

        return BrandingResolverComponentDataHolder.getInstance().getConfigurationManager();
    }

    private BrandingPreference buildBrandingPreferenceFromResource(InputStream inputStream, String type,
                                                                   String name, String locale)
            throws IOException, BrandingPreferenceMgtException {

        String preferencesJSON = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE, name);
        }

        ObjectMapper mapper = new ObjectMapper();
        Object preference = mapper.readValue(preferencesJSON, Object.class);
        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setPreference(preference);
        brandingPreference.setType(type);
        brandingPreference.setName(name);
        brandingPreference.setLocale(locale);
        return brandingPreference;
    }

    private String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    private int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    private String getOrganizationId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
    }

    private String getResourceName(String type, String name, String locale) {

        /*
          Currently, this API provides the support to only configure tenant wise branding preference for 'en-US' locale.
          So always use resource name as default resource name.
          Default resource name is the name used to save organization level branding for 'en-US' language.
         */
        return getTenantId() + RESOURCE_NAME_SEPARATOR + locale;
    }
}
