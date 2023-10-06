/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.core.cache.CacheKey;

/**
 * Cache key for lookup branding resolved tenant from the cache.
 */
public class BrandedOrgCacheKey extends CacheKey {

    private static final long serialVersionUID = -3241022571833301256L;

    private String brandedOrgIdentifier;

    /**
     * @param brandedOrgIdentifier Identifier of the organization that the branding is applied to.
     */
    public BrandedOrgCacheKey(String brandedOrgIdentifier) {

        this.brandedOrgIdentifier = brandedOrgIdentifier;
    }

    /**
     * @return Identifier of the organization that the branding is applied to.
     */
    public String getBrandedOrgIdentifier() {

        return brandedOrgIdentifier;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        BrandedOrgCacheKey that = (BrandedOrgCacheKey) o;

        return brandedOrgIdentifier.equals(that.brandedOrgIdentifier);
    }

    @Override
    public int hashCode() {

        int result = super.hashCode();
        result = 31 * result + brandedOrgIdentifier.hashCode();
        return result;
    }
}
