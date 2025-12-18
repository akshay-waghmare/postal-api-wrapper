package com.mailit.wrapper.exception;

/**
 * Exception thrown when client attempts to access a resource they don't own.
 */
public class ForbiddenException extends WrapperException {
    
    private static final String CODE = "FORBIDDEN";
    private static final int HTTP_STATUS = 403;

    public ForbiddenException(String message) {
        super(CODE, message, HTTP_STATUS);
    }

    public ForbiddenException() {
        super(CODE, "You do not have permission to access this resource", HTTP_STATUS);
    }
}
