/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.branding.preference.management.core.model;

/**
 * A model class representing custom content that includes HTML, CSS, and JS contents for a Custom Layout Defining.
 */
public class CustomContent {

    private String htmlContent;
    private String cssContent;
    private String jsContent;

    /**
     * Constructor to initialize a CustomContent object with HTML, CSS, and JS contents.
     *
     * @param htmlContent The HTML content as a string.
     * @param cssContent The CSS content as a string.
     * @param jsContent The JavaScript content as a string.
     */
    public CustomContent(String htmlContent, String cssContent, String jsContent) {

        this.htmlContent = htmlContent;
        this.cssContent = cssContent;
        this.jsContent = jsContent;
    }

    /**
     * Retrieves the HTML content associated with this object.
     *
     * @return The HTML content as a string.
     */
    public String getHtmlContent() {

        return htmlContent;
    }

    /**
     * Retrieves the CSS content associated with this object.
     *
     * @return The CSS content as a string.
     */
    public String getCssContent() {

        return cssContent;
    }

    /**
     * Retrieves the JavaScript content associated with this object.
     *
     * @return The JavaScript content as a string.
     */
    public String getJsContent() {

        return jsContent;
    }
}
