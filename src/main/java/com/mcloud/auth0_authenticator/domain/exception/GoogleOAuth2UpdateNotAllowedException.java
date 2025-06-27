package com.mcloud.auth0_authenticator.domain.exception;

public class GoogleOAuth2UpdateNotAllowedException extends RuntimeException {
    private final String userId;

    public GoogleOAuth2UpdateNotAllowedException(String userId) {
        super("Não é possível atualizar nome ou email para usuários autenticados via Google OAuth2: " + userId);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}