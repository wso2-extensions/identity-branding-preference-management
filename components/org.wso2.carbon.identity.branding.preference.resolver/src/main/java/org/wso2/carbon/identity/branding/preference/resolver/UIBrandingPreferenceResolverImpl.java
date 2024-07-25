/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtServerException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;
import org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCacheEntry;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCacheKey;
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
import org.wso2.carbon.identity.core.ThreadLocalAwareExecutors;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CUSTOM_TEXT_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CLEARING_BRANDING_PREFERENCE_RESOLVER_CACHE_HIERARCHY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CLEARING_CUSTOM_TEXT_PREFERENCE_RESOLVER_CACHE_HIERARCHY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_APP_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_INVALID_BRANDING_PREFERENCE_TYPE;
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
    private final ExecutorService executorService = ThreadLocalAwareExecutors.newFixedThreadPool(1);
    private static final String ORGANIZATION_DETAILS = "organizationDetails";
    private static final String DISPLAY_NAME = "displayName";

    private final BrandedOrgCache brandedOrgCache;
    private final BrandedAppCache brandedAppCache;
    private final TextCustomizedOrgCache textCustomizedOrgCache;

    /**
     * UI branding preference resolver implementation constructor
     * without application branding resolver cache param.
     *
     * @param brandedOrgCache        Cache instance for branded org.
     * @param textCustomizedOrgCache Cache instance for custom text.
     */
    public UIBrandingPreferenceResolverImpl(BrandedOrgCache brandedOrgCache,
                                            TextCustomizedOrgCache textCustomizedOrgCache) {

        this(brandedOrgCache, BrandedAppCache.getInstance(), textCustomizedOrgCache);
    }

    /**
     * UI branding preference resolver implementation constructor.
     *
     * @param brandedOrgCache        Cache instance for branded org.
     * @param brandedAppCache        Cache instance for branded app.
     * @param textCustomizedOrgCache Cache instance for custom text.
     */
    public UIBrandingPreferenceResolverImpl(BrandedOrgCache brandedOrgCache, BrandedAppCache brandedAppCache,
                                            TextCustomizedOrgCache textCustomizedOrgCache) {

        this.brandedOrgCache = brandedOrgCache;
        this.brandedAppCache = brandedAppCache;
        this.textCustomizedOrgCache = textCustomizedOrgCache;
    }

    @Override
    public BrandingPreference resolveBranding(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        String organizationId = getOrganizationId();
        String currentTenantDomain = getTenantDomain();

        OrganizationManager organizationManager =
                BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();

        if (organizationId == null) {
            try {
                organizationId = organizationManager.resolveOrganizationId(currentTenantDomain);
            } catch (OrganizationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, currentTenantDomain);
            }
        }

        if (APPLICATION_TYPE.equals(type)) {
            return resolveApplicationBranding(name, locale, organizationId, currentTenantDomain);
        } else if (ORGANIZATION_TYPE.equals(type)) {
            return resolveOrganizationBranding(name, locale, organizationId, currentTenantDomain);
        }
        throw handleClientException(ERROR_CODE_INVALID_BRANDING_PREFERENCE_TYPE, type, currentTenantDomain);
    }

    private BrandingPreference resolveOrganizationBranding(String name, String locale,
                                                           String organizationId, String currentTenantDomain)
            throws BrandingPreferenceMgtException {

        OrganizationManager organizationManager =
                BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();
         /* Tenant domain will always be carbon.super for SaaS apps (ex. myaccount). Hence, need to resolve
          tenant domain from the name parameter. */
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(currentTenantDomain)) {
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
                BrandingPreference resolvedBrandingPreference = getPreference(ORGANIZATION_TYPE, name, locale,
                        brandingResolvedTenantDomain);

                if (!currentTenantDomain.equals(brandingResolvedTenantDomain)) {
                    // Since Branding is inherited from an ancestor org, removing the ancestor org displayName.
                    removeOrgDisplayNameFromBrandingPreference(resolvedBrandingPreference);
                }
                return resolvedBrandingPreference;
            }

            // No cache found. Start with current organization.
            Optional<BrandingPreference> brandingPreference =
                    getBrandingPreference(ORGANIZATION_TYPE, name, locale, currentTenantDomain);
            if (brandingPreference.isPresent()) {
                return brandingPreference.get();
            }

            try {
                // There's no need to resolve branding preferences for super tenant since it is the root organization.
                if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(currentTenantDomain)) {
                    List<String> ancestorOrganizationIds =
                            organizationManager.getAncestorOrganizationIds(organizationId);
                    if (CollectionUtils.isEmpty(ancestorOrganizationIds) || ancestorOrganizationIds.size() < 2) {
                        /*  No branding found. Adding the same tenant domain to cache
                          to avoid the resolving in the next run. */
                        addToCache(organizationId, currentTenantDomain, currentTenantDomain);
                        throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED,
                                ORGANIZATION_TYPE, name, currentTenantDomain);
                    }

                    // Get the minimum hierarchy depth that needs to be reached to resolve branding preference
                    int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;
                    for (String ancestorOrgId : ancestorOrganizationIds.subList(1, ancestorOrganizationIds.size())) {
                        String ancestorTenantDomain = organizationManager.resolveTenantDomain(ancestorOrgId);
                        int ancestorDepthInHierarchy =
                                organizationManager.getOrganizationDepthInHierarchy(ancestorOrgId);

                        if (ancestorDepthInHierarchy >= minHierarchyDepth) {
                            brandingPreference =
                                    getBrandingPreference(ORGANIZATION_TYPE, name, locale, ancestorTenantDomain);
                            if (brandingPreference.isPresent()) {
                                /*Since Branding is inherited from an ancestor org,
                                  removing the ancestor org displayName.*/
                                removeOrgDisplayNameFromBrandingPreference(brandingPreference.get());
                                addToCache(organizationId, currentTenantDomain, ancestorTenantDomain);
                                return brandingPreference.get();
                            }
                        } else {
                            break;
                        }
                    }
                }
            } catch (OrganizationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, currentTenantDomain);
            }

            // No branding found. Adding the same tenant domain to cache to avoid the resolving in the next run.
            addToCache(organizationId, currentTenantDomain, currentTenantDomain);
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED,
                    ORGANIZATION_TYPE, name, currentTenantDomain);
        } else {
            // No need to resolve the branding preference. Try to fetch the config from the same org.
            return getPreference(ORGANIZATION_TYPE, name, locale, currentTenantDomain);
        }
    }

    private BrandingPreference resolveApplicationBranding(String appId, String locale,
                                                          String orgId, String currentTenantDomain)
            throws BrandingPreferenceMgtException {

        BrandedAppCacheEntry valueFromCache =
                brandedAppCache.getValueFromCache(new BrandedAppCacheKey(appId), currentTenantDomain);
        if (valueFromCache != null) {
            String brandingResolvedAppId = valueFromCache.getBrandingResolvedAppId();
            String brandingResolvedTenantDomain = valueFromCache.getBrandingResolvedTenant();
            String resolvedBrandingType = valueFromCache.getResolvedBrandingType();

            BrandingPreference resolvedBrandingPreference;
            if (APPLICATION_TYPE.equals(resolvedBrandingType)) {
                resolvedBrandingPreference =
                        getPreference(APPLICATION_TYPE, brandingResolvedAppId, locale, brandingResolvedTenantDomain);
            } else {
                resolvedBrandingPreference =
                        getPreference(ORGANIZATION_TYPE, brandingResolvedTenantDomain, locale,
                                brandingResolvedTenantDomain);
            }
            if (!currentTenantDomain.equals(brandingResolvedTenantDomain)) {
                // Since Branding is inherited from an ancestor org, removing the ancestor org displayName.
                removeOrgDisplayNameFromBrandingPreference(resolvedBrandingPreference);
            }
            return resolvedBrandingPreference;
        }

        // No cache found. Start with current organization application branding.
        Optional<BrandingPreference> brandingPreference =
                getBrandingPreference(APPLICATION_TYPE, appId, locale, currentTenantDomain);
        if (brandingPreference.isPresent()) {
            return brandingPreference.get();
        }

        // No application branding found. Check current organization branding.
        brandingPreference = getBrandingPreference(ORGANIZATION_TYPE, currentTenantDomain, locale,
                currentTenantDomain);
        if (brandingPreference.isPresent()) {
            addAppBrandingToCache(appId, currentTenantDomain, null, currentTenantDomain, ORGANIZATION_TYPE);
            return brandingPreference.get();
        }

        /* It is not possible to resolve application branding further if the organization ID is null or
          if the current tenant domain is super tenant since it is the root organization. */
        if (orgId == null || MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(currentTenantDomain)) {
            // No branding found. Adding the same app id to cache to avoid the resolving in the next run.
            addAppBrandingToCache(appId, currentTenantDomain, appId, currentTenantDomain, APPLICATION_TYPE);
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED,
                    APPLICATION_TYPE, appId, currentTenantDomain);
        }

        try {
            OrganizationManager organizationManager =
                    BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();
            List<String> ancestorOrganizationIds = organizationManager.getAncestorOrganizationIds(orgId);
            if (CollectionUtils.isEmpty(ancestorOrganizationIds) || ancestorOrganizationIds.size() < 2) {
                // No branding found. Adding the same app id to cache to avoid the resolving in the next run.
                addAppBrandingToCache(appId, currentTenantDomain, appId, currentTenantDomain, APPLICATION_TYPE);
                throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED,
                        APPLICATION_TYPE, appId, currentTenantDomain);
            }

            OrgApplicationManager orgApplicationManager =
                    BrandingResolverComponentDataHolder.getInstance().getOrgApplicationManager();
            Map<String, String> ancestorAppIds = orgApplicationManager.getAncestorAppIds(appId, orgId);
            int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;
            for (String ancestorOrgId : ancestorOrganizationIds.subList(1, ancestorOrganizationIds.size())) {
                String ancestorAppId = ancestorAppIds.get(ancestorOrgId);
                String ancestorTenantDomain = organizationManager.resolveTenantDomain(ancestorOrgId);
                int ancestorDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(ancestorOrgId);

                if (ancestorDepthInHierarchy >= minHierarchyDepth) {
                    brandingPreference =
                            getAppBrandingPreferenceFromAncestor(appId, locale, currentTenantDomain, ancestorAppId,
                                    ancestorTenantDomain);
                    if (brandingPreference.isPresent()) {
                        return brandingPreference.get();
                    }
                } else {
                    break;
                }
            }

            // No branding found. Adding the same app id to cache to avoid the resolving in the next run.
            addAppBrandingToCache(appId, currentTenantDomain, appId, currentTenantDomain, APPLICATION_TYPE);
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED,
                    APPLICATION_TYPE, appId, currentTenantDomain);
        } catch (OrganizationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_GETTING_APP_BRANDING_PREFERENCE,
                    appId, currentTenantDomain);
        }
    }

    private Optional<BrandingPreference> getAppBrandingPreferenceFromAncestor(
            String appId, String locale, String currentTenantDomain, String ancestorAppId,
            String ancestorTenantDomain) throws BrandingPreferenceMgtException {

        Optional<BrandingPreference> brandingPreference;
        // If the app is selectively not shared with the ancestor org, ancestor app id can be empty.
        if (StringUtils.isNotBlank(ancestorAppId)) {
            // Check ancestor organization app-level branding.
            brandingPreference = getBrandingPreference(APPLICATION_TYPE, ancestorAppId, locale,
                    ancestorTenantDomain);
            if (brandingPreference.isPresent()) {
                /* Since Branding is inherited from app-level branding of the ancestor org,
                  removing the ancestor org displayName. */
                removeOrgDisplayNameFromBrandingPreference(brandingPreference.get());
                addAppBrandingToCache(appId, currentTenantDomain, ancestorAppId, ancestorTenantDomain,
                        APPLICATION_TYPE);
                return brandingPreference;
            }
        }
        // Since no ancestor organization app-level branding found, check ancestor organization org-level branding.
        brandingPreference =
                getBrandingPreference(ORGANIZATION_TYPE, ancestorTenantDomain, locale,
                        ancestorTenantDomain);
        if (brandingPreference.isPresent()) {
            /* Since Branding is inherited from org-level branding of the parent org,
              removing the ancestor org displayName. */
            removeOrgDisplayNameFromBrandingPreference(brandingPreference.get());
            addAppBrandingToCache(appId, currentTenantDomain, null, ancestorTenantDomain,
                    ORGANIZATION_TYPE);
            return brandingPreference;
        }
        return Optional.empty();
    }

    @Override
    public void clearBrandingResolverCacheHierarchy(String type, String name, String currentTenantDomain)
            throws BrandingPreferenceMgtException {

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

        if (APPLICATION_TYPE.equals(type)) {
            clearAppBrandingResolverCache(currentTenantDomain, name);
        }

        if (organizationId != null) {
            if (ORGANIZATION_TYPE.equals(type)) {
                clearOrgBrandingResolverCache(currentTenantDomain, organizationId);
                brandedAppCache.clear(currentTenantDomain);
            }
            String usernameInContext = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String currentOrgId = organizationId;
            // Clear branding resolver caches by looping through child organization hierarchy.
            CompletableFuture.runAsync(() -> {
                try {
                    clearBrandingResolverCacheHierarchy(type, name, organizationManager, currentOrgId,
                            currentTenantDomain, usernameInContext);
                } catch (BrandingPreferenceMgtServerException e) {
                    LOG.error("An error occurred while clearing branding preference cache hierarchy", e);
                }
            }, executorService);
        }
    }

    /**
     * @deprecated Use {@link #clearBrandingResolverCacheHierarchy(String, String, String)}} instead.
     */
    @Override
    @Deprecated
    public void clearBrandingResolverCacheHierarchy(String currentTenantDomain) throws BrandingPreferenceMgtException {

        clearBrandingResolverCacheHierarchy(ORGANIZATION_TYPE, currentTenantDomain, currentTenantDomain);
    }

    private void clearBrandingResolverCacheHierarchy(String type, String name, OrganizationManager organizationManager,
                                                     String currentOrgId, String currentTenantDomain,
                                                     String usernameInContext)
            throws BrandingPreferenceMgtServerException {

        String cursor = null;
        int pageSize = 10000;
        int iteratorLimit = 100;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(currentTenantDomain, true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(usernameInContext);
            int counter = 0;
            do {
                try {
                    List<BasicOrganization> organizations =
                            organizationManager.getOrganizations(pageSize, cursor, null, "DESC", "", true);
                    Map<String, String> childAppIds = new HashMap<>();
                    if (APPLICATION_TYPE.equals(type)) {
                        OrgApplicationManager orgApplicationManager =
                                BrandingResolverComponentDataHolder.getInstance().getOrgApplicationManager();
                        List<String> childOrgIds = organizations.stream()
                                .map(BasicOrganization::getId)
                                .collect(Collectors.toList());
                        childAppIds = orgApplicationManager.getChildAppIds(name, currentOrgId, childOrgIds);
                    }
                    for (BasicOrganization childOrganization : organizations) {
                        String childTenantDomain = organizationManager.resolveTenantDomain(childOrganization.getId());
                        if (StringUtils.isNotBlank(childTenantDomain)) {
                            if (APPLICATION_TYPE.equals(type)) {
                                String childAppId = childAppIds.get(childOrganization.getId());
                                if (StringUtils.isNotBlank(childAppId)) {
                                    clearAppBrandingResolverCache(childTenantDomain, childAppId);
                                }
                            } else if (ORGANIZATION_TYPE.equals(type)) {
                                clearOrgBrandingResolverCache(childTenantDomain, childOrganization.getId());
                                brandedAppCache.clear(childTenantDomain);
                            }
                        }
                    }
                    cursor = organizations.isEmpty() ? null : Base64.getEncoder().encodeToString(
                            organizations.get(organizations.size() - 1).getCreated().getBytes(StandardCharsets.UTF_8));
                    if (counter > iteratorLimit || counter == 0) {
                        LOG.info("Cursor: " + cursor + ", Organization Size : " + organizations.size()
                                + " and Counter : " + counter);
                    }
                    if (counter > iteratorLimit + 5) {
                        break;
                    }
                } catch (OrganizationManagementException e) {
                    throw handleServerException(ERROR_CODE_ERROR_CLEARING_BRANDING_PREFERENCE_RESOLVER_CACHE_HIERARCHY,
                            currentTenantDomain);
                }
                counter++;
            } while (cursor != null);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void clearOrgBrandingResolverCache(String tenantDomain, String organizationId) {

        BrandedOrgCacheKey brandedOrgCacheKey = new BrandedOrgCacheKey(organizationId);
        BrandedOrgCacheEntry valueFromCache =
                brandedOrgCache.getValueFromCache(brandedOrgCacheKey, tenantDomain);
        if (valueFromCache != null) {
            // If cache exists, clear the cache
            brandedOrgCache.clearCacheEntry(brandedOrgCacheKey, tenantDomain);
        }
    }

    private void clearAppBrandingResolverCache(String tenantDomain, String appId) {

        BrandedAppCacheKey brandedAppCacheKey = new BrandedAppCacheKey(appId);
        BrandedAppCacheEntry valueFromCache =
                brandedAppCache.getValueFromCache(brandedAppCacheKey, tenantDomain);
        if (valueFromCache != null) {
            // If cache exists, clear the cache
            brandedAppCache.clearCacheEntry(brandedAppCacheKey, tenantDomain);
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

    @Override
    public void clearCustomTextResolverCacheHierarchy(String currentTenantDomain, String screen, String locale)
            throws BrandingPreferenceMgtException {

        OrganizationManager organizationManager =
                BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();
        String organizationId = getOrganizationId();
        if (organizationId == null) {
            // If organization id is not available in the context, try to resolve it from tenant domain.
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
        String resourceName = (StringUtils.isNotBlank(screen) && StringUtils.isNotBlank(locale)) ?
                getResourceNameForCustomText(screen, locale) : StringUtils.EMPTY;
        if (organizationId != null) {
            clearCustomTextResolverCache(currentTenantDomain, organizationId, resourceName);
            String usernameInContext = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            // Clear custom text resolver caches by looping through child organization hierarchy.
            CompletableFuture.runAsync(() -> {
                try {
                    clearCustomTextResolverCacheHierarchy(organizationManager, currentTenantDomain, resourceName,
                            usernameInContext);
                } catch (BrandingPreferenceMgtServerException e) {
                    LOG.error("An error occurred while clearing custom text preference cache hierarchy", e);
                }
            }, executorService);
        }
    }

    private void clearCustomTextResolverCacheHierarchy(OrganizationManager organizationManager,
                                                       String currentTenantDomain, String resourceName,
                                                       String usernameInContext)
            throws BrandingPreferenceMgtServerException {

        String cursor = null;
        int pageSize = 10000;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(currentTenantDomain, true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(usernameInContext);
            do {
                try {
                    List<BasicOrganization> organizations =
                            organizationManager.getOrganizations(pageSize, cursor, null, "DESC", "", true);
                    for (BasicOrganization childOrganization : organizations) {
                        String childTenantDomain = organizationManager.resolveTenantDomain(childOrganization.getId());
                        if (StringUtils.isNotBlank(childTenantDomain)) {
                            clearCustomTextResolverCache(childTenantDomain, childOrganization.getId(), resourceName);
                        }
                    }
                    cursor = organizations.isEmpty() ? null : Base64.getEncoder().encodeToString(
                            organizations.get(organizations.size() - 1).getCreated().getBytes(StandardCharsets.UTF_8));
                } catch (OrganizationManagementException e) {
                    throw handleServerException(
                            ERROR_CODE_ERROR_CLEARING_CUSTOM_TEXT_PREFERENCE_RESOLVER_CACHE_HIERARCHY,
                            currentTenantDomain);
                }
            } while (cursor != null);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void clearCustomTextResolverCache(String tenantDomain, String organizationId, String resourceName) {

        // If resourceName is empty, clear all the custom text cache entries for the current tenant domain.
        if (StringUtils.isBlank(resourceName)) {
            textCustomizedOrgCache.clear(tenantDomain);
            return;
        }
        TextCustomizedOrgCacheKey cacheKey = new TextCustomizedOrgCacheKey(organizationId, resourceName);
        TextCustomizedOrgCacheEntry valueFromCache =
                textCustomizedOrgCache.getValueFromCache(cacheKey, tenantDomain);
        if (valueFromCache != null) {
            // If cache exists, clear the cache.
            textCustomizedOrgCache.clearCacheEntry(cacheKey, tenantDomain);
        }
    }

    private void addToCache(String brandedOrgId, String brandedTenantDomain, String brandingInheritedTenantDomain) {

        BrandedOrgCacheKey cacheKey = new BrandedOrgCacheKey(brandedOrgId);
        BrandedOrgCacheEntry cacheEntry = new BrandedOrgCacheEntry(brandingInheritedTenantDomain);
        brandedOrgCache.addToCache(cacheKey, cacheEntry, brandedTenantDomain);
    }

    private void addAppBrandingToCache(String appId, String tenantDomain, String brandingInheritedAppId,
                                       String brandingInheritedTenantDomain, String resolvedBrandingType) {

        BrandedAppCacheKey cacheKey = new BrandedAppCacheKey(appId);
        BrandedAppCacheEntry cacheEntry =
                new BrandedAppCacheEntry(brandingInheritedTenantDomain, brandingInheritedAppId,
                        resolvedBrandingType);
        brandedAppCache.addToCache(cacheKey, cacheEntry, tenantDomain);
    }

    private Optional<BrandingPreference> getBrandingPreference(String type, String name, String locale,
                                                               String tenantDomain)
            throws BrandingPreferenceMgtException {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

            String resourceName = getResourceName(type, name, locale);
            String resourceType = getResourceType(type);
            List<ResourceFile> resourceFiles = getConfigurationManager().getFiles(resourceType, resourceName);
            if (resourceFiles.isEmpty()) {
                return Optional.empty();
            }
            if (StringUtils.isBlank(resourceFiles.get(0).getId())) {
                return Optional.empty();
            }

            InputStream inputStream = getConfigurationManager().getFileById
                    (resourceType, resourceName, resourceFiles.get(0).getId());
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

        if (APPLICATION_TYPE.equals(type)) {
            return name.toLowerCase() + RESOURCE_NAME_SEPARATOR + locale;
        }
        return getTenantId() + RESOURCE_NAME_SEPARATOR + locale;
    }

    /**
     * Return resource type for the given branding type.
     *
     * @param type Branding Type(Organization branding/Application branding).
     * @return resource type of the branding resource.
     */
    private String getResourceType(String type) {

        if (APPLICATION_TYPE.equals(type)) {
            return APPLICATION_BRANDING_RESOURCE_TYPE;
        }
        return BRANDING_RESOURCE_TYPE;
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
                () -> handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, type, name,
                        currentTenantDomain));
    }

    /**
     * Remove the display name of the organization from the branding preference.
     *
     * @param brandingPreference Branding preference.
     */
    private void removeOrgDisplayNameFromBrandingPreference(BrandingPreference brandingPreference) {

        ((LinkedHashMap) ((LinkedHashMap) brandingPreference.getPreference()).get(ORGANIZATION_DETAILS))
                .replace(DISPLAY_NAME, StringUtils.EMPTY);
    }
}
