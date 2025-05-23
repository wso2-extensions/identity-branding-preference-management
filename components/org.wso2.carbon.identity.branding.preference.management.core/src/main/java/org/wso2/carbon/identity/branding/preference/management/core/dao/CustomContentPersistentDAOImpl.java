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

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT;
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
    public void addCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                                 String tenantDomain) throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully added.", tenantDomain));
            }
            try {
                orgCustomContentDAO.addOrgCustomContent(customLayoutContent, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT, tenantDomain, e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " + "successfully added.",
                        applicationUuid, tenantDomain));
            }
            try {
                appCustomContentDAO.addAppCustomContent(customLayoutContent, applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
        }
    }

    @Override
    public void updateCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                                    String tenantDomain) throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully updated.", tenantDomain));
            }
            try {
                orgCustomContentDAO.updateOrgCustomContent(customLayoutContent, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, tenantDomain, e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " + "successfully updated.",
                        applicationUuid, tenantDomain));
            }
            try {
                appCustomContentDAO.updateAppCustomContent(customLayoutContent, applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
        }
    }

    @Override
    public boolean isCustomContentExists(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);

        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Custom Layout content existence for tenant: %s is successfully checked.",
                        tenantDomain));
            }
            try {
                return orgCustomContentDAO.isOrgCustomContentAvailable(tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS, tenantDomain, e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content existence for application: %s for tenant: %s " +
                                "is successfully checked.", applicationUuid, tenantDomain));
            }
            try {
                return appCustomContentDAO.isAppCustomContentAvailable(applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS,
                        applicationUuid, e);
            }
        }
    }

    @Override
    public CustomLayoutContent getCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully retrieved.", tenantDomain));
            }
            try {
                return orgCustomContentDAO.getOrgCustomContent(tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT,
                        tenantDomain, e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout Content for application: %s for tenant: %s " +
                                "successfully retrieved.", applicationUuid, tenantDomain));
            }
            try {
                return appCustomContentDAO.getAppCustomContent(applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
        }
    }

    @Override
    public void deleteCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException {

        int tenantId = getTenantId(tenantDomain);
        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for tenant: %s successfully deleted.", tenantDomain));
            }
            try {
                orgCustomContentDAO.deleteOrgCustomContent(tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, tenantDomain, e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Custom Layout content for application: %s for tenant: %s " +
                                "successfully deleted.", applicationUuid, tenantDomain));
            }
            try {
                appCustomContentDAO.deleteAppCustomContent(applicationUuid, tenantId);
            } catch (BrandingPreferenceMgtException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, applicationUuid, e);
            }
        }
    }
}
