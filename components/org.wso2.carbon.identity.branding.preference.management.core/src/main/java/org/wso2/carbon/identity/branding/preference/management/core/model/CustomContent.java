package org.wso2.carbon.identity.branding.preference.management.core.model;

import java.io.InputStream;

public class CustomContent {

    private String htmlContent;
    private String cssContent;
    private String jsContent;

    public CustomContent(String htmlContent, String cssContent, String jsContent) {

        this.htmlContent = htmlContent;
        this.cssContent = cssContent;
        this.jsContent = jsContent;
    }
    public CustomContent() {}

    public String getHtmlContent() {

        return htmlContent;
    }

    public String getCssContent() {

        return cssContent;
    }

    public String getJsContent() {

        return jsContent;
    }
}
