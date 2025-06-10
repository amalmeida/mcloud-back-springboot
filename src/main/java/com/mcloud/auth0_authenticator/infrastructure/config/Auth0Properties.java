package com.mcloud.auth0_authenticator.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth0")
public class Auth0Properties {

    private Management management = new Management();
    private String namespace;

    @Data
    public static class Management {
        private String domain;
        private String clientId;
        private String clientSecret;
    }
}
