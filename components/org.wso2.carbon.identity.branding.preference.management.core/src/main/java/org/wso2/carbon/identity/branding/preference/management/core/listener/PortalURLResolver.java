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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManagerImpl;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.flow.execution.engine.listener.AbstractFlowExecutionListener;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;

import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.buildConfiguredPortalURL;

/**
 * This class is responsible for injecting the portal URL during flow execution.
 */
public class PortalURLResolver extends AbstractFlowExecutionListener {

    private static final Log LOG = LogFactory.getLog(PortalURLResolver.class);
    private final BrandingPreferenceManagerImpl brandingPreferenceManager;

    public PortalURLResolver(BrandingPreferenceManagerImpl brandingPreferenceManager) {

        this.brandingPreferenceManager = brandingPreferenceManager;
    }

    @Override
    public int getExecutionOrderId() {

        return 4;
    }

    @Override
    public int getDefaultOrderId() {

        return 4;
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    @Override
    public boolean doPreExecute(FlowExecutionContext context) {

        String flowType = context.getFlowType();
        if (StringUtils.isNotBlank(context.getPortalUrl())) {
            return true;
        }
        String applicationId = context.getApplicationId();
        String tenantDomain = context.getTenantDomain();
        try {
            String configuredPortalURL = buildConfiguredPortalURL(applicationId, tenantDomain,
                    brandingPreferenceManager, flowType);
            context.setPortalUrl(configuredPortalURL);
        } catch (URLBuilderException e) {
            LOG.error("Error building default portal URL for tenant: " + tenantDomain, e);
            return false;
        } catch (BrandingPreferenceMgtException e) {
            LOG.error("Error retrieving branding preference for tenant: " + tenantDomain, e);
            return false;
        }
        return true;
    }
}
