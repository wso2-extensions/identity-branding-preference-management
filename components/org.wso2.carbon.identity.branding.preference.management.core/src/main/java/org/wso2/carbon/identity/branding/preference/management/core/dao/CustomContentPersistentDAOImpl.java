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

package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;
import static org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantId;

/**
 * CustomContentPersistentDAOImpl is responsible for handling operations related to custom layout content persistence.
 */
public class CustomContentPersistentDAOImpl implements CustomContentPersistentDAO {

    private static final Log log = LogFactory.getLog(CustomContentPersistentDAOImpl.class);

    private final OrgCustomContentDAOImpl orgCustomContentDAO = new OrgCustomContentDAOImpl();
    private final AppCustomContentDAOImpl appCustomContentDAO = new AppCustomContentDAOImpl();

    @Override
    public void addOrUpdateCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                                         String tenantDomain) throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        try {
            if (!isCustomContentExists(applicationUuid, tenantDomain)) {
                // DAO impl adds the content if not exists
                if (StringUtils.isBlank(applicationUuid)) {
                    // DAO impl adds the content to ORG table if applicationUuid is blank.
                    orgCustomContentDAO.addOrgCustomContent(customLayoutContent, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Custom Layout content for tenant: %s successfully added.", tenantDomain));
                    }
                } else {
                    // DAO impl adds the content to APP table if applicationUuid is not blank.
                    appCustomContentDAO.addAppCustomContent(customLayoutContent, applicationUuid, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Custom Layout content for application: %s for tenant: %s " +
                                        "successfully added.", applicationUuid, tenantDomain));
                    }
                }
            } else {
                // DAO impl updates the content if exists
                if (StringUtils.isBlank(applicationUuid)) {
                    orgCustomContentDAO.updateOrgCustomContent(customLayoutContent, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Custom Layout content for tenant: %s successfully updated.",
                                tenantDomain));
                    }
                } else {
                    appCustomContentDAO.updateAppCustomContent(customLayoutContent, applicationUuid, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Custom Layout content for application: %s for tenant: %s " +
                                        "successfully updated.",
                                applicationUuid, tenantDomain));
                    }
                }
            }
        } catch (BrandingPreferenceMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_CUSTOM_LAYOUT_CONTENT, e);
        }
    }

    @Override
    public boolean isCustomContentExists(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        try {
            if (StringUtils.isBlank(applicationUuid)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content existence for tenant: %s is successfully checked.", tenantDomain));
                }
                return orgCustomContentDAO.isOrgCustomContentAvailable(tenantId);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content existence for application: %s for tenant: %s " +
                                    "is successfully checked.", applicationUuid, tenantDomain));
                }
                return appCustomContentDAO.isAppCustomContentAvailable(applicationUuid, tenantId);
            }
        } catch (BrandingPreferenceMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS, e);
        }
    }

    @Override
    public CustomLayoutContent getCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        try {
            if (StringUtils.isBlank(applicationUuid)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for tenant: %s successfully retrieved.", tenantDomain));
                }
                return orgCustomContentDAO.getOrgCustomContent(tenantId);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout Content for application: %s for tenant: %s " +
                                    "successfully retrieved.", applicationUuid, tenantDomain));
                }
                return appCustomContentDAO.getAppCustomContent(applicationUuid, tenantId);
            }
        } catch (BrandingPreferenceMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT, e);
        }
    }

    @Override
    public void deleteCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        try {
            if (StringUtils.isBlank(applicationUuid)) {
                orgCustomContentDAO.deleteOrgCustomContent(tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for tenant: %s successfully deleted.", tenantDomain));
                }
            } else {
                appCustomContentDAO.deleteAppCustomContent(applicationUuid, tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Custom Layout content for application: %s for tenant: %s " +
                                    "successfully deleted.", applicationUuid, tenantDomain));
                }
            }
        } catch (BrandingPreferenceMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, e);
        }
    }
}
