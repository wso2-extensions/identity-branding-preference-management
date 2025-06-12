/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManager;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManagerImpl;
import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.management.core.ai.BrandingAIPreferenceManager;
import org.wso2.carbon.identity.branding.preference.management.core.ai.BrandingAIPreferenceManagerImpl;
import org.wso2.carbon.identity.branding.preference.management.core.listener.PortalURLResolver;
import org.wso2.carbon.identity.branding.preference.management.core.listener.IdentityTenantMgtListener;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.flow.execution.engine.listener.FlowExecutionListener;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

/**
 * OSGi declarative services component which handles registration and un-registration of branding preference management
 * service.
 */
@Component(
        name = "branding.preference.mgt.component",
        immediate = true
)
public class BrandingPreferenceManagerComponent {

    private static final Log LOG = LogFactory.getLog(BrandingPreferenceManagerComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            BrandingPreferenceManagerImpl brandingPreferenceManager = new BrandingPreferenceManagerImpl();
            context.getBundleContext()
                    .registerService(BrandingPreferenceManager.class, brandingPreferenceManager, null);
            context.getBundleContext()
                    .registerService(BrandingAIPreferenceManager.class.getName(), new BrandingAIPreferenceManagerImpl(),
                            null);
            context.getBundleContext()
                    .registerService(TenantMgtListener.class.getName(), new IdentityTenantMgtListener(), null);
            context.getBundleContext()
                    .registerService(FlowExecutionListener.class.getName(),
                            new PortalURLResolver(brandingPreferenceManager), null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("BrandingPreferenceMgt Service Component is activated.");
            }
        } catch (Exception e) {
            LOG.error("Error while activating the branding preference manager component.");
        }
    }

    @Reference(
            name = "resource.configuration.manager.service",
            service = ConfigurationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationManager"
    )
    protected void setConfigurationManager(ConfigurationManager configurationManager) {

        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the ConfigurationManager.");
        }
    }

    protected void unsetConfigurationManager(ConfigurationManager configurationManager) {

        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting the ConfigurationManager.");
        }
    }

    @Reference(
            name = "identity.event.service",
            service = IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService"
    )
    protected void setIdentityEventService(IdentityEventService identityEventService) {

        BrandingPreferenceManagerComponentDataHolder.getInstance().setIdentityEventService(identityEventService);
        if (LOG.isDebugEnabled()) {
            LOG.debug("IdentityEventService set in Branding Preference Management bundle");
        }
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {

        BrandingPreferenceManagerComponentDataHolder.getInstance().setIdentityEventService(null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("IdentityEventService unset in Branding Preference Management bundle");
        }
    }

    @Reference(
            name = "ui.branding.preference.resolver",
            service = UIBrandingPreferenceResolver.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unUIBrandingResolveService"
    )
    protected void setUIBrandingResolveService(UIBrandingPreferenceResolver uiBrandingPreferenceResolver) {

        BrandingPreferenceManagerComponentDataHolder.getInstance()
                .setUiBrandingPreferenceResolver(uiBrandingPreferenceResolver);
        if (LOG.isDebugEnabled()) {
            LOG.debug("UIBrandingPreferenceResolver set in Branding Preference Management bundle");
        }
    }

    protected void unUIBrandingResolveService(UIBrandingPreferenceResolver identityEventService) {

        BrandingPreferenceManagerComponentDataHolder.getInstance().setIdentityEventService(null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("UIBrandingPreferenceResolver unset in Branding Preference Management bundle");
        }
    }
}
