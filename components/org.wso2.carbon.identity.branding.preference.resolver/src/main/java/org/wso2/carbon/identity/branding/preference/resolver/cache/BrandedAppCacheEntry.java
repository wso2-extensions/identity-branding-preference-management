/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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
 * Cache entry which is kept in the branding resolved app cache.
 */
public class BrandedAppCacheEntry extends CacheEntry {

    private static final long serialVersionUID = 3112605038259278888L;

    private String brandingResolvedTenant;
    private String brandingResolvedAppId;
    private String resolvedBrandingType;

    /**
     * @param brandingResolvedTenant Domain of the tenant that branding is resolved from.
     * @param brandingResolvedAppId  App ID of the app that branding is resolved from.
     * @param resolvedBrandingType   Type of the branding that is resolved.
     */
    public BrandedAppCacheEntry(String brandingResolvedTenant, String brandingResolvedAppId,
                                String resolvedBrandingType) {

        this.brandingResolvedTenant = brandingResolvedTenant;
        this.brandingResolvedAppId = brandingResolvedAppId;
        this.resolvedBrandingType = resolvedBrandingType;
    }

    /**
     * @return Domain of the tenant that branding is resolved from.
     */
    public String getBrandingResolvedTenant() {

        return brandingResolvedTenant;
    }

    /**
     * @param brandingResolvedTenant Domain of the tenant that branding is resolved from.
     */
    public void setBrandingResolvedTenant(String brandingResolvedTenant) {

        this.brandingResolvedTenant = brandingResolvedTenant;
    }

    /**
     * @return App ID of the app that branding is resolved from.
     */
    public String getBrandingResolvedAppId() {

        return brandingResolvedAppId;
    }

    /**
     * @param brandingResolvedAppId App ID of the app that branding is resolved from.
     */
    public void setBrandingResolvedAppId(String brandingResolvedAppId) {

        this.brandingResolvedAppId = brandingResolvedAppId;
    }

    /**
     * @return Type of the branding that is resolved.
     */
    public String getResolvedBrandingType() {

        return resolvedBrandingType;
    }

    /**
     * @param resolvedBrandingType Type of the branding that is resolved.
     */
    public void setResolvedBrandingType(String resolvedBrandingType) {

        this.resolvedBrandingType = resolvedBrandingType;
    }
}
