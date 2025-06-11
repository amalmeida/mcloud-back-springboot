package com.mcloud.auth0_authenticator.domain.exception;

public enum ErrorCode {
    USER_NOT_FOUND("error.user.not.found"),
    AUTH0_UPDATE_FAILED("error.auth0.update.failed"),
    AUTH0_USER_NOT_FOUND("error.auth0.user.not.found"),
    AUTH0_UNAUTHORIZED("error.auth0.unauthorized"),
    VALIDATION_ERROR("error.validation"),
    INVALID_JSON("error.invalid.json"),
    GENERIC_ERROR("error.generic");

    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}