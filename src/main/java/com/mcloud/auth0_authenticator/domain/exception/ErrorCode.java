package com.mcloud.auth0_authenticator.domain.exception;

public enum ErrorCode {
    USER_NOT_FOUND("error.user.notfound"),
    AUTH0_UPDATE_FAILED("error.auth0.update"),
    AUTH0_ROLES_FAILED("error.auth0.roles"),
    AUTH0_PERMISSIONS_FAILED("error.auth0.permissions"),
    VALIDATION_ERROR("error.validation"),
    GENERIC_ERROR("error.generic"),
    INVALID_JSON("error.invalid.json");

    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}