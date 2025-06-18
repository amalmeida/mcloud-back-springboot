package com.mcloud.auth0_authenticator.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserType {
    PESSOA_FISICA("PF"),
    PESSOA_JURIDICA("PJ");

    private final String code;

    UserType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static UserType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserType type : UserType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid UserType code: " + code);
    }
}