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
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.flow.execution.engine.listener.AbstractFlowExecutionListener;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;

import java.util.Map;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_URLS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;

/**
 * This class is responsible for handling the branding preference management during flow execution.
 */
public class PortalURLResolver extends AbstractFlowExecutionListener {

    private static final Log log = LogFactory.getLog(PortalURLResolver.class);
    private final BrandingPreferenceManagerImpl brandingPreferenceManager;
    public static final String SELF_SIGN_UP_URL = "selfSignUpURL";
    public static final String DEFAULT_REGISTRATION_PORTAL_URL = "/authenticationendpoint/register.do";
    public static final String REGISTRATION = "REGISTRATION";

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

        try {
            if (StringUtils.isNotBlank(context.getPortalUrl())) {
                return true;
            }

            String applicationId = context.getApplicationId();
            String tenantDomain = context.getTenantDomain();
            String type = StringUtils.isBlank(applicationId) ? ORGANIZATION_TYPE : APPLICATION_TYPE;
            String name = StringUtils.isBlank(applicationId) ? tenantDomain : applicationId;

            BrandingPreference preference = brandingPreferenceManager.getBrandingPreference(type, name, DEFAULT_LOCALE);

            if (preference != null) {
                Map<String, Object> prefMap = (Map<String, Object>) preference.getPreference();
                Map<String, String> urlMap = (Map<String, String>) prefMap.get(BRANDING_URLS);

                if (REGISTRATION.equals(context.getFlowType())) {
                    String signUpUrl = (urlMap != null) ? urlMap.get(SELF_SIGN_UP_URL) : null;

                    if (StringUtils.isNotBlank(signUpUrl)) {
                        context.setPortalUrl(signUpUrl);
                    } else {
                        log.debug("Self sign-up URL not configured for tenant: " + tenantDomain +
                                ". Using default URL.");
                        context.setPortalUrl(buildDefaultRegistrationUrl());
                    }
                }
            }
            if (StringUtils.isBlank(context.getPortalUrl())) {
                log.debug(String.format("No branding preference found for type: %s, name: %s, tenant: %s." +
                        " Using default URL.", type, name, tenantDomain));
                context.setPortalUrl(buildDefaultRegistrationUrl());
            }
            return true;
        } catch (BrandingPreferenceMgtClientException e) {
            log.debug("Self sign-up URL not configured for tenant: " + context.getTenantDomain() +
                    ". Using default URL.");
            try {
                context.setPortalUrl(buildDefaultRegistrationUrl());
            } catch (URLBuilderException ex) {
                log.error("Failed to build default registration URL for tenant: " + context.getTenantDomain(), ex);
                return false;
            }
            return true;

        } catch (BrandingPreferenceMgtException e) {
            log.error("Error retrieving branding preference for tenant: " + context.getTenantDomain(), e);
            return false;

        } catch (URLBuilderException e) {
            log.error("Error building default registration portal URL for tenant: " + context.getTenantDomain(), e);
            return false;
        }
    }

    private String buildDefaultRegistrationUrl() throws URLBuilderException {

        return ServiceURLBuilder.create()
                .addPath(DEFAULT_REGISTRATION_PORTAL_URL)
                .build()
                .getAbsolutePublicURL();
    }
}
