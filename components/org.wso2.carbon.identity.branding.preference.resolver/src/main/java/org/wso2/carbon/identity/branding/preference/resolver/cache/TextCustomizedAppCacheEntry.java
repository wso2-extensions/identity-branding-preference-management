/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.resolver.cache;

import org.wso2.carbon.identity.core.cache.CacheEntry;

/**
 * Cache entry which is kept in the application custom text resolved source cache.
 */
public class TextCustomizedAppCacheEntry extends CacheEntry {

    private static final long serialVersionUID = 4529113607723394167L;

    private final String customTextResolvedTenant;
    private final String customTextResolvedAppId;
    private final String resolvedCustomTextType;

    /**
     * @param customTextResolvedTenant Domain of the tenant that the custom text is resolved from.
     * @param customTextResolvedAppId  ID of the application that the custom text is resolved from.
     *                                 Null if the custom text is resolved from an organization.
     * @param resolvedCustomTextType   Type of the resolved custom text (APP or ORG).
     */
    public TextCustomizedAppCacheEntry(String customTextResolvedTenant, String customTextResolvedAppId,
                                       String resolvedCustomTextType) {

        this.customTextResolvedTenant = customTextResolvedTenant;
        this.customTextResolvedAppId = customTextResolvedAppId;
        this.resolvedCustomTextType = resolvedCustomTextType;
    }

    /**
     * @return Domain of the tenant that the custom text is resolved from.
     */
    public String getCustomTextResolvedTenant() {

        return customTextResolvedTenant;
    }

    /**
     * @return ID of the application that the custom text is resolved from.
     */
    public String getCustomTextResolvedAppId() {

        return customTextResolvedAppId;
    }

    /**
     * @return Type of the resolved custom text (APP or ORG).
     */
    public String getResolvedCustomTextType() {

        return resolvedCustomTextType;
    }
}
