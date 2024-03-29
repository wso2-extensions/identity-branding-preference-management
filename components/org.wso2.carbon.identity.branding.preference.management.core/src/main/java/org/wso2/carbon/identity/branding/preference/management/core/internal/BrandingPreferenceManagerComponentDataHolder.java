/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.event.services.IdentityEventService;

/**
 * A class to keep the data of the branding preference management component.
 */
public class BrandingPreferenceManagerComponentDataHolder {

    private static final BrandingPreferenceManagerComponentDataHolder brandingPreferenceManagerComponentDataHolder =
            new BrandingPreferenceManagerComponentDataHolder();

    private ConfigurationManager configurationManager;
    private IdentityEventService identityEventService;
    private UIBrandingPreferenceResolver uiBrandingPreferenceResolver;

    private BrandingPreferenceManagerComponentDataHolder() {

    }

    public static BrandingPreferenceManagerComponentDataHolder getInstance() {

        return brandingPreferenceManagerComponentDataHolder;
    }

    public ConfigurationManager getConfigurationManager() {

        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {

        this.configurationManager = configurationManager;
    }

    /**
     * Get identity event service reference.
     *
     * @return {@link IdentityEventService}.
     */
    public IdentityEventService getIdentityEventService() {

        return identityEventService;
    }

    /**
     * Set identity event service.
     *
     * @param identityEventService Identity Event Service.
     */
    public void setIdentityEventService(IdentityEventService identityEventService) {

        this.identityEventService = identityEventService;
    }

    public UIBrandingPreferenceResolver getUiBrandingPreferenceResolver() {

        return uiBrandingPreferenceResolver;
    }

    public void setUiBrandingPreferenceResolver(UIBrandingPreferenceResolver uiBrandingPreferenceResolver) {

        this.uiBrandingPreferenceResolver = uiBrandingPreferenceResolver;
    }
}
