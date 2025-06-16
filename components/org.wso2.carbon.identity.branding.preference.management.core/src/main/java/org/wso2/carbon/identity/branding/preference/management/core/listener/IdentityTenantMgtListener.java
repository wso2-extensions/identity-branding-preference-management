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

package org.wso2.carbon.identity.branding.preference.management.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.impl.CustomContentPersistentFactory;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.core.AbstractIdentityTenantMgtListener;

import static org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantDomain;

/**
 * IdentityTenantMgtListener for Branding Preference Management.
 */
public class IdentityTenantMgtListener extends AbstractIdentityTenantMgtListener {

    private static final Log LOG = LogFactory.getLog(IdentityTenantMgtListener.class);

    @Override
    public void onTenantDelete(int tenantId) {

        String tenantDomain = getTenantDomain(tenantId);
        try {
            getCustomContentPersistentDAO().deleteCustomContent(null, tenantDomain);
        } catch (BrandingPreferenceMgtException e) {
            // Log the exception and continue with the tenant deletion.
            // This is to ensure that tenant deletion is not blocked due to an error in custom content deletion.
            // The error can be logged for further investigation.
            LOG.error(String.format("Error occurred while deleting custom content for tenant: %s", tenantDomain), e);
        }
    }

    /**
     * Get the CustomContentPersistentDAO instance.
     *
     * @return CustomContentPersistentDAO instance.
     */
    private CustomContentPersistentDAO getCustomContentPersistentDAO() {

        return CustomContentPersistentFactory.getCustomContentPersistentDAO();
    }
}
