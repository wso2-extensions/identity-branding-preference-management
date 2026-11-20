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

import org.wso2.carbon.identity.core.cache.CacheKey;

/**
 * Cache key for the application custom text resolved source cache.
 */
public class TextCustomizedAppCacheKey extends CacheKey {

    private static final long serialVersionUID = -2873406148306515402L;

    private final String appId;

    private final String resourceName;

    /**
     * @param appId        ID of the application that the custom text is requested for.
     * @param resourceName Screen and locale specific custom text resource name.
     */
    public TextCustomizedAppCacheKey(String appId, String resourceName) {

        this.appId = appId;
        this.resourceName = resourceName;
    }

    /**
     * @return ID of the application that the custom text is requested for.
     */
    public String getAppId() {

        return appId;
    }

    /**
     * @return Screen and locale specific custom text resource name.
     */
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

        TextCustomizedAppCacheKey that = (TextCustomizedAppCacheKey) o;
        if (!appId.equals(that.appId)) {
            return false;
        }
        return resourceName.equals(that.resourceName);
    }

    @Override
    public int hashCode() {

        int result = super.hashCode();
        result = 31 * result + appId.hashCode();
        result = 31 * result + resourceName.hashCode();
        return result;
    }
}
