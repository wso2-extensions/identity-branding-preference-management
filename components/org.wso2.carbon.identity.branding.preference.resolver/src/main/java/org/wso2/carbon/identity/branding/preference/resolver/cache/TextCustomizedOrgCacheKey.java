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
 * Cache key for lookup custom text resolved tenant from the cache.
 */
public class TextCustomizedOrgCacheKey extends CacheKey {

    private static final long serialVersionUID = -3241022571833301256L;

    private String textCustomizedOrgIdentifier;

    private String resourceName;

    /**
     * @param textCustomizedOrgIdentifier Identifier of the organization that the custom text is applied to.
     * @param resourceName                Resource name of the custom text resource. Unique to the screen & locale.
     */
    public TextCustomizedOrgCacheKey(String textCustomizedOrgIdentifier, String resourceName) {

        this.textCustomizedOrgIdentifier = textCustomizedOrgIdentifier;
        this.resourceName = resourceName;
    }

    /**
     * @return Identifier of the organization that the custom text is applied to.
     */
    public String getTextCustomizedOrgIdentifier() {

        return textCustomizedOrgIdentifier;
    }

    public String getResourceName() {

        return resourceName;
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

        TextCustomizedOrgCacheKey that = (TextCustomizedOrgCacheKey) o;

        if (!textCustomizedOrgIdentifier.equals(that.textCustomizedOrgIdentifier)) {
            return false;
        }
        return resourceName.equals(that.resourceName);
    }

    @Override
    public int hashCode() {

        int result = super.hashCode();
        result = 31 * result + textCustomizedOrgIdentifier.hashCode();
        result = 31 * result + resourceName.hashCode();
        return result;
    }
}
