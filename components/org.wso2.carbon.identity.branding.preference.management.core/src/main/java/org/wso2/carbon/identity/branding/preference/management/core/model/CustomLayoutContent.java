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

package org.wso2.carbon.identity.branding.preference.management.core.model;

import org.apache.commons.lang.StringUtils;

/**
 * Model class representing the layout content that includes HTML, CSS, and JS contents for a Custom Layout Defining.
 */
public class CustomLayoutContent {

    private final String html;
    private final String css;
    private final String js;

    private CustomLayoutContent(String html, String css, String js) {

        this.html = html;
        this.css = StringUtils.isNotBlank(css) ? css : null;
        this.js = StringUtils.isNotBlank(js) ? js : null;
    }

    /**
     * Builder class for constructing instances of CustomLayoutContent.
     */
    public static class CustomLayoutContentBuilder {

        private String html;
        private String css;
        private String js;

        /**
         * Sets the HTML content for the CustomLayoutContent.
         *
         * @param html The HTML content as a string.
         * @return The builder instance for method chaining.
         */
        public CustomLayoutContentBuilder setHtml(String html) {

            this.html = html;
            return this;
        }

        /**
         * Sets the CSS content for the CustomLayoutContent.
         *
         * @param css The CSS content as a string.
         * @return The builder instance for method chaining.
         */
        public CustomLayoutContentBuilder setCss(String css) {

            this.css = css;
            return this;
        }

        /**
         * Sets the JavaScript content for the CustomLayoutContent.
         *
         * @param js The JavaScript content as a string.
         * @return The builder instance for method chaining.
         */
        public CustomLayoutContentBuilder setJs(String js) {

            this.js = js;
            return this;
        }

        /**
         * Builds an instance of CustomLayoutContent with the provided HTML, CSS, and JS content.
         *
         * @return A new instance of CustomLayoutContent.
         */
        public CustomLayoutContent build() {

            if (StringUtils.isBlank(html)) {
                return null;
            }
            return new CustomLayoutContent(html, css, js);
        }
    }

    /**
     * Retrieves the HTML content associated with this object.
     *
     * @return The HTML content as a string.
     */
    public String getHtml() {

        return html;
    }

    /**
     * Retrieves the CSS content associated with this object.
     *
     * @return The CSS content as a string.
     */
    public String getCss() {

        return css;
    }

    /**
     * Retrieves the JavaScript content associated with this object.
     *
     * @return The JavaScript content as a string.
     */
    public String getJs() {

        return js;
    }
}
