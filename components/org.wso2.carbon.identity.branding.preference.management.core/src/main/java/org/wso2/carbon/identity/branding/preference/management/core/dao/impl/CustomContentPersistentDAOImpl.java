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
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.AppCustomContentCacheKey;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.CustomContentCache;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.CustomContentCacheEntry;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.OrgCustomContentCacheKey;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;
import static org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantId;

/**
 * CustomContentPersistentDAOImpl is responsible for handling operations related to custom layout content persistence.
 */
class CustomContentPersistentDAOImpl implements CustomContentPersistentDAO {

    private static final Log log = LogFactory.getLog(CustomContentPersistentDAOImpl.class);

    private final OrgCustomContentDAOImpl orgCustomContentDAO = new OrgCustomContentDAOImpl();
    private final AppCustomContentDAOImpl appCustomContentDAO = new AppCustomContentDAOImpl();
    private final CustomContentCache customContentCache = CustomContentCache.getInstance();

    @Override
    public void addCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                                 String tenantDomain) throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            try {
                orgCustomContentDAO.addOrgCustomContent(customLayoutContent, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT, tenantDomain, e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully added.", tenantDomain));
            }
        } else {
            try {
                appCustomContentDAO.addAppCustomContent(customLayoutContent, applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
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
            try {
                orgCustomContentDAO.updateOrgCustomContent(customLayoutContent, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, tenantDomain, e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully updated.", tenantDomain));
            }
        } else {
            try {
                appCustomContentDAO.updateAppCustomContent(customLayoutContent, applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
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
        CustomLayoutContent cachedContent = getCacheEntry(applicationUuid, tenantDomain);
        if (cachedContent != null) {
            return cachedContent;
        }

        int tenantId = getTenantId(tenantDomain);
        CustomLayoutContent customLayoutContent = null;
        if (StringUtils.isBlank(applicationUuid)) {
            try {
                customLayoutContent = orgCustomContentDAO.getOrgCustomContent(tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT,
                        tenantDomain, e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully retrieved.", tenantDomain));
            }
        } else {
            try {
                customLayoutContent = appCustomContentDAO.getAppCustomContent(applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
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
            try {
                orgCustomContentDAO.deleteOrgCustomContent(tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, tenantDomain, e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully deleted.", tenantDomain));
            }
        } else {
            try {
                appCustomContentDAO.deleteAppCustomContent(applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " +
                                "successfully deleted.", applicationUuid, tenantDomain));
            }
        }
        clearCache(applicationUuid, tenantDomain);
    }

    private void clearCache(String applicationUuid, String tenantDomain) {

        if (StringUtils.isBlank(applicationUuid)) {
            customContentCache.clearCacheEntry(new OrgCustomContentCacheKey(tenantDomain), tenantDomain);
        } else {
            customContentCache.clearCacheEntry(new AppCustomContentCacheKey(applicationUuid, tenantDomain),
                    tenantDomain);
        }
    }

    private CustomLayoutContent getCacheEntry(String applicationUuid, String tenantDomain) {

        CustomContentCacheEntry cacheEntry;
        if (StringUtils.isBlank(applicationUuid)) {
            cacheEntry = customContentCache.getValueFromCache(new OrgCustomContentCacheKey(tenantDomain),
                    tenantDomain);
            if (cacheEntry != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for tenant: %s retrieved from cache.", tenantDomain));
                }
                return cacheEntry.getCustomLayoutContent();
            }
        } else {
            cacheEntry =
                    customContentCache.getValueFromCache(new AppCustomContentCacheKey(applicationUuid, tenantDomain),
                            tenantDomain);
            if (cacheEntry != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for application: %s for tenant: %s retrieved from cache.",
                            applicationUuid, tenantDomain));
                }
                return cacheEntry.getCustomLayoutContent();
            }
        }
        return null;
    }

    private void addCustomContentToCache(CustomLayoutContent customLayoutContent, String applicationUuid,
            String tenantDomain) {

        CustomContentCacheEntry cacheEntry = new CustomContentCacheEntry(customLayoutContent);
        if (StringUtils.isBlank(applicationUuid)) {
            customContentCache.addToCache(new OrgCustomContentCacheKey(tenantDomain), cacheEntry, tenantDomain);
        } else {
            customContentCache.addToCache(new AppCustomContentCacheKey(applicationUuid, tenantDomain), cacheEntry,
                    tenantDomain);
        }
    }
}
