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

package org.wso2.carbon.identity.branding.preference.management.core.dao.constants;

/**
 * DataBase Table Columns management related constants
 */

public class DaoConstants {
    /**
     * Grouping of constants related to database table names.
     */
    public static class CustomContentTableColumns {

        public static final String ID = "ID";
        public static final String TENANT_ID = "TENANT_ID";
        public static final String CONTENT = "CONTENT";
        public static final String CONTENT_TYPE = "CONTENT_TYPE";
        public static final String APP_ID = "APP_ID";
        public static final String CREATED_AT = "CREATED_AT";
        public static final String UPDATED_AT = "UPDATED_AT";
    }

    /**
     * Defines constants for custom content types.
     * - CONTENT_TYPE_HTML: Represents HTML content.
     * - CONTENT_TYPE_CSS: Represents CSS content.
     * - CONTENT_TYPE_JS: Represents JavaScript content.
     */
    public static class CustomContentTypes {

        public static final String CONTENT_TYPE_HTML = "html";
        public static final String CONTENT_TYPE_CSS = "css";
        public static final String CONTENT_TYPE_JS = "js";
    }
}
