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
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;
import org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCacheEntry;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCacheKey;
import org.wso2.carbon.identity.branding.preference.resolver.cache.TextCustomizedOrgCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.TextCustomizedOrgCacheEntry;
import org.wso2.carbon.identity.branding.preference.resolver.cache.TextCustomizedOrgCacheKey;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CUSTOM_TEXT_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CLEARING_BRANDING_PREFERENCE_RESOLVER_CACHE_HIERARCHY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NAME_SEPARATOR;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.getFormattedLocale;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleClientException;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;

/**
 * UI Branding Preference Resolver Implementation.
 */
public class UIBrandingPreferenceResolverImpl implements UIBrandingPreferenceResolver {

    private static final Log LOG = LogFactory.getLog(UIBrandingPreferenceResolverImpl.class);

    private final BrandedOrgCache brandedOrgCache;
    private final TextCustomizedOrgCache textCustomizedOrgCache;

    /**
     * UI branding preference resolver implementation constructor.
     *
     * @param brandedOrgCache Cache instance.
     * @param textCustomizedOrgCache Cache instance for custom text.
     */
    public UIBrandingPreferenceResolverImpl(BrandedOrgCache brandedOrgCache,
                                            TextCustomizedOrgCache textCustomizedOrgCache) {

        this.brandedOrgCache = brandedOrgCache;
        this.textCustomizedOrgCache = textCustomizedOrgCache;
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
                String brandingResolvedTenantDomain = valueFromCache.getBrandingResolvedTenant();
                BrandingPreference resolvedBrandingPreference = getPreference(type, name, locale,
                        brandingResolvedTenantDomain);

                if (!currentTenantDomain.equals(brandingResolvedTenantDomain)) {
                    // Since Branding is inherited from Parent org, removing the Parent org displayName.
                    ((LinkedHashMap) ((LinkedHashMap) resolvedBrandingPreference
                            .getPreference()).get("organizationDetails"))
                            .replace("displayName", StringUtils.EMPTY);
                }
                return resolvedBrandingPreference;
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
                    // This is a root tenant.
                    if (StringUtils.isBlank(parentId)) {
                        return getPreference(type, name, locale, currentTenantDomain);
                    }
                    String parentTenantDomain = organizationManager.resolveTenantDomain(parentId);
                    int parentDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(parentId);

                    // Get the minimum hierarchy depth that needs to be reached to resolve branding preference
                    int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;

                    while (parentDepthInHierarchy >= minHierarchyDepth) {
                        brandingPreference =
                                getBrandingPreference(type, name, locale, parentTenantDomain);
                        if (brandingPreference.isPresent()) {
                            // Since Branding is inherited from Parent org, removing the Parent org displayName.
                            ((LinkedHashMap) ((LinkedHashMap) brandingPreference.get()
                                    .getPreference()).get("organizationDetails"))
                                    .replace("displayName", StringUtils.EMPTY);
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
            return getPreference(type, name, locale, currentTenantDomain);
        }
    }

    @Override
    public void clearBrandingResolverCacheHierarchy(String currentTenantDomain) throws BrandingPreferenceMgtException {

        OrganizationManager organizationManager =
                BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();
        String organizationId = getOrganizationId();
        if (organizationId == null) {
            // If organization id is not available in the context, try to resolve it from tenant domain
            try {
                organizationId = organizationManager.resolveOrganizationId(currentTenantDomain);
            } catch (OrganizationManagementException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error occurred while resolving organization Id for tenant domain: "
                            + currentTenantDomain, e);
                }
                return;
            }
        }

        List<String> childOrganizationIds = new ArrayList<>();
        childOrganizationIds.add(organizationId);

        // Clear branding resolver caches by looping (breadth-first) through child organization hierarchy
        while (!childOrganizationIds.isEmpty()) {
            // Pop the first child organization Id from the list
            String childOrganizationId = childOrganizationIds.remove(0);
            BrandedOrgCacheKey brandedOrgCacheKey = new BrandedOrgCacheKey(childOrganizationId);

            try {
                String childTenantDomain = organizationManager.resolveTenantDomain(childOrganizationId);
                BrandedOrgCacheEntry valueFromCache =
                        brandedOrgCache.getValueFromCache(brandedOrgCacheKey, childTenantDomain);
                if (valueFromCache != null) {
                    // If cache exists, clear the cache
                    brandedOrgCache.clearCacheEntry(brandedOrgCacheKey, childTenantDomain);
                }

                // Add Ids of all child organizations of the current (child) organization
                childOrganizationIds.addAll(organizationManager.getChildOrganizationsIds(childOrganizationId));
            } catch (OrganizationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_CLEARING_BRANDING_PREFERENCE_RESOLVER_CACHE_HIERARCHY,
                        currentTenantDomain);
            }
        }
    }
  
    @Override  
    public CustomText resolveCustomText(String type, String name, String screen, String locale)
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
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE, currentTenantDomain);
            }
        }

        if (organizationId != null) {
            String resourceName = getResourceNameForCustomText(screen, locale);
            TextCustomizedOrgCacheEntry valueFromCache = textCustomizedOrgCache.getValueFromCache
                    (new TextCustomizedOrgCacheKey(organizationId, resourceName), currentTenantDomain);
            if (valueFromCache != null) {
                Optional<CustomText> customText =
                        getCustomText(type, name, screen, locale, valueFromCache.getCustomTextResolvedTenant());
                return customText.orElseThrow(
                        () -> handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, getTenantDomain()));
            }

            // No cache found. Start with current organization.
            Optional<CustomText> customText = getCustomText(type, name, screen, locale, currentTenantDomain);
            if (customText.isPresent()) {
                return customText.get();
            }

            try {
                Organization organization = organizationManager.getOrganization(organizationId, false, false);
                // There's no need to resolve branding preferences for super tenant since it is the root organization.
                if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(currentTenantDomain)) {
                    // Get the details of the parent organization and resolve the custom text preferences.
                    String parentId = organization.getParent().getId();
                    String parentTenantDomain = organizationManager.resolveTenantDomain(parentId);
                    int parentDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(parentId);

                    // Get the minimum hierarchy depth that needs to be reached to resolve branding preference.
                    int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;

                    while (parentDepthInHierarchy >= minHierarchyDepth) {
                        customText = getCustomText(type, name, screen, locale, parentTenantDomain);
                        if (customText.isPresent()) {
                            addCustomTextResolvedOrgToCache
                                    (organizationId, resourceName, currentTenantDomain, parentTenantDomain);
                            return customText.get();
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
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE, getTenantDomain());
            }

            // No custom text found. Adding the same tenant domain to cache to avoid the resolving in the next run.
            addCustomTextResolvedOrgToCache(organizationId, resourceName, currentTenantDomain, currentTenantDomain);
            throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, getTenantDomain());
        } else {
            // No need to resolve the custom text preference. Try to fetch the config from the same org.
            Optional<CustomText> customText = getCustomText(type, name, screen, locale, currentTenantDomain);
            return customText.orElseThrow(
                    () -> handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, getTenantDomain()));
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

    /**
     * Add custom text resolved tenant to cache.
     *
     * @param textCustomizedOrgId             Text customized organization id.
     * @param resourceName                    Resource name of the custom text resource. Unique to the screen & locale.
     * @param textCustomizedTenantDomain      Text customized tenant domain.
     * @param customTextInheritedTenantDomain Custom text inherited tenant domain.
     */
    private void addCustomTextResolvedOrgToCache(String textCustomizedOrgId, String resourceName,
                                                 String textCustomizedTenantDomain,
                                                 String customTextInheritedTenantDomain) {

        TextCustomizedOrgCacheKey cacheKey = new TextCustomizedOrgCacheKey(textCustomizedOrgId, resourceName);
        TextCustomizedOrgCacheEntry cacheEntry = new TextCustomizedOrgCacheEntry(customTextInheritedTenantDomain);
        textCustomizedOrgCache.addToCache(cacheKey, cacheEntry, textCustomizedTenantDomain);
    }

    /**
     * Retrieve a custom text preference by calling configuration-mgt service.
     *
     * @param type   Type of the custom text preference.
     * @param name   Name of the tenant/application where custom text belongs.
     * @param locale Language preference of the custom text.
     * @param screen Screen where the custom text needs to be applied.
     * @return The requested custom text preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    private Optional<CustomText> getCustomText(String type, String name, String screen, String locale,
                                               String tenantDomain)
            throws BrandingPreferenceMgtException {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

            String resourceName = getResourceNameForCustomText(screen, locale);
            List<ResourceFile> resourceFiles = getConfigurationManager().getFiles
                    (CUSTOM_TEXT_RESOURCE_TYPE, resourceName);
            if (resourceFiles.isEmpty()) {
                return Optional.empty();
            }
            if (StringUtils.isBlank(resourceFiles.get(0).getId())) {
                return Optional.empty();
            }

            InputStream inputStream = getConfigurationManager().getFileById
                    (CUSTOM_TEXT_RESOURCE_TYPE, resourceName, resourceFiles.get(0).getId());
            if (inputStream == null) {
                return Optional.empty();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Custom text preference for tenant: " + tenantDomain + " is retrieved successfully.");
            }
            return Optional.of(buildCustomTextFromResource(inputStream, type, name, screen, locale));
        } catch (ConfigurationManagementException e) {
            if (!RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE, tenantDomain, e);
            }
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE, tenantDomain);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return Optional.empty();
    }

    /**
     * Build a Custom Text Model from custom text preference file stream.
     *
     * @param inputStream Preference file stream.
     * @param type        Custom Text resource type.
     * @param name        Tenant/Application name.
     * @param screen      Screen Name.
     * @param locale      Language preference.
     * @return Custom Text Preference.
     */
    private CustomText buildCustomTextFromResource(InputStream inputStream, String type, String name,
                                                   String screen, String locale)
            throws IOException, BrandingPreferenceMgtException {

        String preferencesJSON = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE, name);
        }

        ObjectMapper mapper = new ObjectMapper();
        Object preference = mapper.readValue(preferencesJSON, Object.class);
        CustomText customText = new CustomText();
        customText.setPreference(preference);
        customText.setType(type);
        customText.setName(name);
        customText.setLocale(locale);
        customText.setScreen(screen);
        return customText;
    }

    /**
     * Generate and return resource name of the custom text resource.
     *
     * @param screen Screen name where the custom texts need to be applied.
     * @param locale Language preference
     * @return resource name for the custom text preference.
     */
    private String getResourceNameForCustomText(String screen, String locale) {

        String formattedLocale = getFormattedLocale(locale);
        return StringUtils.upperCase(screen) + RESOURCE_NAME_SEPARATOR + StringUtils.lowerCase(formattedLocale);
    }


    /**
     * Retrieve a branding preference by calling configuration-mgt service.
     *
     * @param type   Type of the branding preference.
     * @param name   Name of the tenant/application where branding belongs.
     * @param locale Language preference of the branding.
     * @return The requested branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    private BrandingPreference getPreference(String type, String name, String locale, String currentTenantDomain)
            throws BrandingPreferenceMgtException {

        Optional<BrandingPreference> brandingPreference =
                getBrandingPreference(type, name, locale, currentTenantDomain);
        return brandingPreference.orElseThrow(
                () -> handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS, getTenantDomain()));
    }
}
