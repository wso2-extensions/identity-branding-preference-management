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

package org.wso2.carbon.identity.branding.preference.management.core.exception;

/**
 * Represents a custom exception specific to content-related server-side errors in the branding preference
 * management feature. This exception extends the {@link BrandingPreferenceMgtServerException} class to handle
 * scenarios requiring custom server-side error definitions.
 */
public class CustomContentServerException extends BrandingPreferenceMgtServerException {

    public CustomContentServerException() {

        super();
    }

    public CustomContentServerException(String message, String errorCode) {

        super(message, errorCode);
    }

    public CustomContentServerException(String message, String errorCode, Throwable cause) {

        super(message, errorCode, cause);
    }

    public CustomContentServerException(Throwable cause) {

        super(cause);
    }
}
