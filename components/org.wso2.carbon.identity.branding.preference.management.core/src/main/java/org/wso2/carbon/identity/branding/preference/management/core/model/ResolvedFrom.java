/*
 * Copyright (c) 2025-2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.model;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;

/**
 * A model class representing a resolved from.
 */
public class ResolvedFrom {

    private String type;
    private String organization;
    private String application;

    /**
     * @deprecated Use {@link #ResolvedFrom(String, String, String)} instead.
     */
    @Deprecated
    public ResolvedFrom(String type, String name) {

        this.type = type;
        if (APPLICATION_TYPE.equals(type)) {
            this.application = name;
        } else {
            this.organization = name;
        }
    }

    public ResolvedFrom(String type, String organization, String application) {

        this.type = type;
        this.organization = organization;
        this.application = application;
    }

    public String getType() {

        return type;
    }

    /**
     * Get the resource name of the source the preference was resolved from. The returned value depends on the type:
     * the organization for the ORG type and the application for the APP type.
     *
     * @deprecated Use {@link #getOrganization()} and {@link #getApplication()} instead.
     */
    @Deprecated
    public String getName() {

        return APPLICATION_TYPE.equals(type) ? application : organization;
    }

    /**
     * Get the organization the preference was resolved from. Available for all the types.
     *
     * @return Organization the preference was resolved from.
     */
    public String getOrganization() {

        return organization;
    }

    /**
     * Get the application the preference was resolved from. Available only for the APP type.
     *
     * @return Application the preference was resolved from.
     */
    public String getApplication() {

        return application;
    }
}
