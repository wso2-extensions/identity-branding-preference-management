package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.branding.preference.management.core.exception.CustomContentServerException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomContent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantId;

public class CustomContentDAO implements CustomContentPersistentManager{

    private static final Log log = LogFactory.getLog(CustomContentDAO.class);

    private final OrgCustomContentDAO orgCustomContentDAO = new OrgCustomContentDAO();
    private final AppCustomContentDAO appCustomContentDAO = new AppCustomContentDAO();

    @Override
    public void addOrUpdateCustomContent(CustomContent customContent, String applicationUuid, String tenantDomain) throws CustomContentServerException {
        int tenantId = getTenantId(tenantDomain);

        try{
            if (!isCustomContentExists(applicationUuid, tenantDomain)) {
                if (StringUtils.isBlank(applicationUuid)) {
                    orgCustomContentDAO.addOrgCustomContent(customContent, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Org %s template with locale: %s for type: %s for tenant: %s successfully added.", tenantDomain));
                    }
                } else {
                    appCustomContentDAO.addAppCustomContent(customContent, applicationUuid, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                        "successfully added.", applicationUuid, tenantDomain));
                    }
                }
            } else {
                // DAO impl updates the template if exists
                if (StringUtils.isBlank(applicationUuid)) {
                    orgCustomContentDAO.updateOrgCustomContent(customContent, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Org %s template with locale: %s for type: %s for tenant: %s successfully updated.",
                                tenantDomain));
                    }
                } else {
                    appCustomContentDAO.updateAppCustomContent(customContent, applicationUuid, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                        "successfully updated.",
                                applicationUuid, tenantDomain));
                    }
                }
            }
        }catch (CustomContentServerException e){
            throw new CustomContentServerException(e);
        }
    }

    @Override
    public boolean isCustomContentExists(String applicationUuid, String tenantDomain) throws CustomContentServerException {
        int tenantId = getTenantId(tenantDomain);

        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for type: %s for tenant: %s successfully added.", tenantDomain));
            }
            return orgCustomContentDAO.isOrgCustomContentAvailable(tenantId);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                "successfully added.", applicationUuid, tenantDomain));
            }
            return appCustomContentDAO.isAppCustomContentAvailable(applicationUuid, tenantId);
        }
    }

    @Override
    public CustomContent getCustomContent(String applicationUuid, String tenantDomain) throws CustomContentServerException {
        int tenantId = getTenantId(tenantDomain);

        if (StringUtils.isBlank(applicationUuid)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for type: %s for tenant: %s successfully added.", tenantDomain));
            }
            return orgCustomContentDAO.getOrgCustomContent(tenantId);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                "successfully added.", applicationUuid, tenantDomain));
            }
            return appCustomContentDAO.getAppCustomContent(applicationUuid, tenantId);
        }
    }

    @Override
    public void deleteCustomContent(String applicationUuid, String tenantDomain) throws CustomContentServerException {
        int tenantId = getTenantId(tenantDomain);

        if (StringUtils.isBlank(applicationUuid)) {
            orgCustomContentDAO.deleteOrgCustomContent(tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org %s template with locale: %s for type: %s for tenant: %s successfully added.", tenantDomain));
            }
        } else {
            appCustomContentDAO.deleteAppCustomContent(applicationUuid, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App %s template with locale: %s for type: %s for application: %s for tenant: %s " +
                                "successfully added.", applicationUuid, tenantDomain));
            }
        }
    }
}
