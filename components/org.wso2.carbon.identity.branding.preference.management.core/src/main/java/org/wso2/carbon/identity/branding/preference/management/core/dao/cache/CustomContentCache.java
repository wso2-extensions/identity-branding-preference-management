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

import org.wso2.carbon.identity.core.cache.BaseCache;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Cache for custom content.
 */
public class CustomContentCache extends BaseCache<OrgCustomContentCacheKey, CustomContentCacheEntry> {

    private static final String CACHE_NAME = "BrandingCustomContentCache";

    private static final CustomContentCache INSTANCE = new CustomContentCache();

    private CustomContentCache() {

        super(CACHE_NAME);
    }

    /**
     * Get the custom content cache instance.
     *
     * @return Custom content cache instance.
     */
    public static CustomContentCache getInstance() {

        CarbonUtils.checkSecurity();
        return INSTANCE;
    }
}
