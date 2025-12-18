package com.mailit.wrapper.exception;

/**
 * Base exception for all wrapper API errors.
 */
public abstract class WrapperException extends RuntimeException {
    
    private final String code;
    private final int httpStatus;

    protected WrapperException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    protected WrapperException(String code, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
