package com.mailit.wrapper.exception;

/**
 * Exception thrown when API key authentication fails.
 */
public class AuthenticationException extends WrapperException {
    
    private static final String CODE = "UNAUTHORIZED";
    private static final int HTTP_STATUS = 401;

    public AuthenticationException() {
        super(CODE, "Invalid or missing API key", HTTP_STATUS);
    }

    public AuthenticationException(String message) {
        super(CODE, message, HTTP_STATUS);
    }
}
