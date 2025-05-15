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
