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

package org.wso2.carbon.identity.branding.preference.resolver.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.resolver.UIBrandingPreferenceResolverImpl;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.TextCustomizedOrgCache;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * OSGi declarative services component which handles registration and un-registration of branding preference management
 * service.
 */
@Component(
        name = "branding.preference.resolver.component",
        immediate = true
)
public class BrandingResolverComponent {

    private static final Log LOG = LogFactory.getLog(BrandingResolverComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(UIBrandingPreferenceResolver.class,
                    new UIBrandingPreferenceResolverImpl(BrandedOrgCache.getInstance(),
                            BrandedAppCache.getInstance(), TextCustomizedOrgCache.getInstance()), null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("BrandingResolver Component is activated.");
            }
        } catch (Exception e) {
            LOG.error("Error while activating the branding resolver component.");
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

        BrandingResolverComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the ConfigurationManager.");
        }
    }

    protected void unsetConfigurationManager(ConfigurationManager configurationManager) {

        BrandingResolverComponentDataHolder.getInstance().setConfigurationManager(null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting the ConfigurationManager.");
        }
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        BrandingResolverComponentDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        BrandingResolverComponentDataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "identity.organization.application.management.component",
            service = OrgApplicationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgApplicationManager"
    )
    protected void setOrgApplicationManager(OrgApplicationManager orgApplicationManager) {

        BrandingResolverComponentDataHolder.getInstance().setOrgApplicationManager(orgApplicationManager);
    }

    protected void unsetOrgApplicationManager(OrgApplicationManager orgApplicationManager) {

        BrandingResolverComponentDataHolder.getInstance().setOrgApplicationManager(null);
    }
}
