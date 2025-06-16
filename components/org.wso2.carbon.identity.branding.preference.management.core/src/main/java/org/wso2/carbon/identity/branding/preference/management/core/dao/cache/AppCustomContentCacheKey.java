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

package org.wso2.carbon.identity.branding.preference.management.core.dao.cache;

import java.util.Objects;

/**
 * Cache key for application custom content.
 * This is used to cache custom content related to a specific application.
 */
public class AppCustomContentCacheKey extends OrgCustomContentCacheKey {

    private static final long serialVersionUID = -582394857239485723L;
    private final String appId;

    public AppCustomContentCacheKey(String tenantDomain, String appId) {

        super(tenantDomain);
        this.appId = appId;
    }

    /**
     * Get the application ID.
     *
     * @return Application ID.
     */
    public String getAppId() {

        return appId;
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof AppCustomContentCacheKey)) {
            return false;
        }
        AppCustomContentCacheKey that = (AppCustomContentCacheKey) o;
        return getTenantDomain().equals(that.getTenantDomain()) && appId.equals(that.getAppId());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getTenantDomain(), appId);
    }
}
