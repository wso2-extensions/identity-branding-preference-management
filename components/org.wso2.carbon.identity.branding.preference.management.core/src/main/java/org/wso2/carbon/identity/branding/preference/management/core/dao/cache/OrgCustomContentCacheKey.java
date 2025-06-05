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

import org.wso2.carbon.identity.core.cache.CacheKey;

/**
 * Cache key for organization custom content.
 * This is used to cache custom content related to a specific tenant domain.
 */
public class OrgCustomContentCacheKey extends CacheKey {

    private static final long serialVersionUID = -3478291045829374012L;
    private final String tenantDomain;

    public OrgCustomContentCacheKey(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    /**
     * Get the tenant domain.
     *
     * @return Tenant domain.
     */
    public String getTenantDomain() {

        return tenantDomain;
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof OrgCustomContentCacheKey)) {
            return false;
        }
        return tenantDomain.equals(((OrgCustomContentCacheKey) o).getTenantDomain());
    }

    @Override
    public int hashCode() {

        return tenantDomain.hashCode();
    }
}
