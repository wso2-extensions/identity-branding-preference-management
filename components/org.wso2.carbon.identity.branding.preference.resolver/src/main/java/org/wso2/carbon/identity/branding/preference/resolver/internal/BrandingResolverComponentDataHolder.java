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

package org.wso2.carbon.identity.branding.preference.resolver.internal;

import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * A class to keep the data of the branding preference management component.
 */
public class BrandingResolverComponentDataHolder {

    private static final BrandingResolverComponentDataHolder brandingPreferenceManagerComponentDataHolder =
            new BrandingResolverComponentDataHolder();

    private ConfigurationManager configurationManager;
    private OrganizationManager organizationManager;


    public static BrandingResolverComponentDataHolder getInstance() {

        return brandingPreferenceManagerComponentDataHolder;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(
            OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    private BrandingResolverComponentDataHolder() {

    }

    public ConfigurationManager getConfigurationManager() {

        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {

        this.configurationManager = configurationManager;
    }
}
