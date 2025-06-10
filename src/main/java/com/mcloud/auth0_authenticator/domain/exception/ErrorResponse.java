package com.mcloud.auth0_authenticator.domain.exception;

public class ErrorResponse {
    private final String code;
    private final String message;

    public ErrorResponse(ErrorCode errorCode, String message) {
        this.code = errorCode.name();
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
