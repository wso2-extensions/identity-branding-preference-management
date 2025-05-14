package org.wso2.carbon.identity.branding.preference.management.core.exception;

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
