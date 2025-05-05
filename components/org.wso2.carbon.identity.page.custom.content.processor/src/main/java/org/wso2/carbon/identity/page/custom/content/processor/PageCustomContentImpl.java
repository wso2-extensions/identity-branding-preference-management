package org.wso2.carbon.identity.page.custom.content.processor;

import org.wso2.carbon.identity.page.custom.content.processor.cache.CustomContentAppCache;
import org.wso2.carbon.identity.page.custom.content.processor.cache.CustomContentOrgCache;

/**
 * Constants related to branding preference management.
 */

public class PageCustomContentImpl {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }

    private final CustomContentAppCache customContentAppCache;
    private final CustomContentOrgCache customContentOrgCache;

    /**
     * Page Custom Content implementation constructor
     * without application branding resolver cache param.
     *
     * @param customContentOrgCache        Cache instance for branded org.
     */

    public PageCustomContentImpl(CustomContentAppCache customContentAppCache, CustomContentOrgCache customContentOrgCache) {
        this.customContentAppCache = customContentAppCache;
        this.customContentOrgCache = customContentOrgCache;
    }


}
