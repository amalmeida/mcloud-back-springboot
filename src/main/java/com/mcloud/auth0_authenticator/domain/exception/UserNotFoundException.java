package com.mcloud.auth0_authenticator.domain.exception;

public class UserNotFoundException extends RuntimeException {
    private final String userId;

    public UserNotFoundException(String userId) {
        super(ErrorCode.USER_NOT_FOUND.getMessageKey());
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}