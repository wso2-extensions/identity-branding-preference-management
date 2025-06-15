/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.dao.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.branding.preference.management.core.dao.AppCustomContentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.OrgCustomContentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.AppCustomContentCacheKey;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.CustomContentCache;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.CustomContentCacheEntry;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.OrgCustomContentCacheKey;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;

import static org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantId;

/**
 * CustomContentPersistentDAOImpl is responsible for handling operations related to custom layout content persistence.
 */
class CustomContentPersistentDAOImpl implements CustomContentPersistentDAO {

    private static final Log log = LogFactory.getLog(CustomContentPersistentDAOImpl.class);

    @Override
    public void addCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                                 String tenantDomain) throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            getOrgCustomContentDAO().addOrgCustomContent(customLayoutContent, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully added.", tenantDomain));
            }
        } else {
            getAppCustomContentDAO().addAppCustomContent(customLayoutContent, applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " + "successfully added.",
                        applicationUuid, tenantDomain));
            }
        }
    }

    @Override
    public void updateCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                                    String tenantDomain) throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            getOrgCustomContentDAO().updateOrgCustomContent(customLayoutContent, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully updated.", tenantDomain));
            }
        } else {
            getAppCustomContentDAO().updateAppCustomContent(customLayoutContent, applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " + "successfully updated.",
                        applicationUuid, tenantDomain));
            }
        }
        clearCache(applicationUuid, tenantDomain);
    }

    @Override
    public CustomLayoutContent getCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        // Check if the content is already cached.
        CustomContentCacheEntry cacheEntry = getCacheEntry(applicationUuid, tenantDomain);
        if (cacheEntry != null) {
            return cacheEntry.getCustomLayoutContent();
        }

        int tenantId = getTenantId(tenantDomain);
        CustomLayoutContent customLayoutContent = null;
        if (StringUtils.isBlank(applicationUuid)) {
            customLayoutContent = getOrgCustomContentDAO().getOrgCustomContent(tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully retrieved.", tenantDomain));
            }
        } else {
            customLayoutContent = getAppCustomContentDAO().getAppCustomContent(applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout Content for application: %s for tenant: %s " +
                                "successfully retrieved.", applicationUuid, tenantDomain));
            }
        }
        addCustomContentToCache(customLayoutContent, applicationUuid, tenantDomain);
        return customLayoutContent;
    }

    @Override
    public void deleteCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            getOrgCustomContentDAO().deleteOrgCustomContent(tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully deleted.", tenantDomain));
            }
        } else {
            getAppCustomContentDAO().deleteAppCustomContent(applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " +
                                "successfully deleted.", applicationUuid, tenantDomain));
            }
        }
        clearCache(applicationUuid, tenantDomain);
    }

    /**
     * Clears the cache for the specified application UUID and tenant domain.
     *
     * @param applicationUuid Application UUID, if applicable.
     * @param tenantDomain    Tenant domain.
     */
    private void clearCache(String applicationUuid, String tenantDomain) {

        if (StringUtils.isBlank(applicationUuid)) {
            getCustomContentCache().clearCacheEntry(new OrgCustomContentCacheKey(tenantDomain), tenantDomain);
        } else {
            getCustomContentCache().clearCacheEntry(new AppCustomContentCacheKey(applicationUuid, tenantDomain),
                    tenantDomain);
        }
    }

    /**
     * Retrieves the cached custom layout content for the specified application UUID and tenant domain.
     *
     * @param applicationUuid Application UUID, if applicable.
     * @param tenantDomain    Tenant domain.
     * @return Custom cache entry containing the custom layout content, or null if not found.
     */
    private CustomContentCacheEntry getCacheEntry(String applicationUuid, String tenantDomain) {

        CustomContentCacheEntry cacheEntry;
        if (StringUtils.isBlank(applicationUuid)) {
            cacheEntry = getCustomContentCache().getValueFromCache(new OrgCustomContentCacheKey(tenantDomain),
                    tenantDomain);
            if (cacheEntry != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for tenant: %s retrieved from cache.", tenantDomain));
                }
            }
        } else {
            cacheEntry = getCustomContentCache().getValueFromCache(
                    new AppCustomContentCacheKey(applicationUuid, tenantDomain), tenantDomain);
            if (cacheEntry != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for application: %s for tenant: %s retrieved from cache.",
                            applicationUuid, tenantDomain));
                }
            }
        }
        return cacheEntry;
    }

    /**
     * Adds custom content to the cache.
     *
     * @param customLayoutContent Custom layout content to be cached.
     * @param applicationUuid     Application UUID, if applicable.
     * @param tenantDomain        Tenant domain.
     */
    private void addCustomContentToCache(CustomLayoutContent customLayoutContent, String applicationUuid,
            String tenantDomain) {

        CustomContentCacheEntry cacheEntry = new CustomContentCacheEntry(customLayoutContent);
        if (StringUtils.isBlank(applicationUuid)) {
            getCustomContentCache().addToCache(new OrgCustomContentCacheKey(tenantDomain), cacheEntry, tenantDomain);
        } else {
            getCustomContentCache().addToCache(new AppCustomContentCacheKey(applicationUuid, tenantDomain), cacheEntry,
                    tenantDomain);
        }
    }

    /**
     * Get the singleton instance of CustomContentPersistentDAOImpl.
     *
     * @return CustomContentPersistentDAOImpl instance.
     */
    private OrgCustomContentDAO getOrgCustomContentDAO() {

        return OrgCustomContentDAOImpl.getInstance();
    }

    /**
     * Get the singleton instance of AppCustomContentDAOImpl.
     *
     * @return AppCustomContentDAOImpl instance.
     */
    private AppCustomContentDAO getAppCustomContentDAO() {

        return AppCustomContentDAOImpl.getInstance();
    }

    /**
     * Get the singleton instance of CustomContentCache.
     *
     * @return CustomContentCache instance.
     */
    private CustomContentCache getCustomContentCache() {

        return CustomContentCache.getInstance();
    }
}
