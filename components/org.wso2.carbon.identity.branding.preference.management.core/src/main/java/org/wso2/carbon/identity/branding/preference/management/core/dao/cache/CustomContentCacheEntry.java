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

import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.core.cache.CacheEntry;

/**
 * Cache entry for custom content.
 */
public class CustomContentCacheEntry extends CacheEntry {

    private static final long serialVersionUID = -4578392012345678901L;
    private CustomLayoutContent customLayoutContent;

    public CustomContentCacheEntry(CustomLayoutContent customLayoutContent) {

        this.customLayoutContent = customLayoutContent;
    }

    /**
     * Get the custom layout content.
     *
     * @return Custom layout content.
     */
    public CustomLayoutContent getCustomLayoutContent() {

        return customLayoutContent;
    }

    /**
     * Set the custom layout content.
     *
     * @param customLayoutContent Custom layout content.
     */
    public void setCustomLayoutContent(CustomLayoutContent customLayoutContent) {

        this.customLayoutContent = customLayoutContent;
    }
}
