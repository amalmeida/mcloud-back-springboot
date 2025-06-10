package com.mcloud.auth0_authenticator.infrastructure.config;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class Auth0ManagementConfig {

    private final Auth0Properties auth0Properties;

    private static final Logger logger = LoggerFactory.getLogger(Auth0ManagementConfig.class);


    public Auth0ManagementConfig(Auth0Properties auth0Properties) {
        this.auth0Properties = auth0Properties;
    }

    @Bean
    public ManagementAPI managementAPI() throws Auth0Exception {
        String domain = auth0Properties.getManagement().getDomain();
        String clientId = auth0Properties.getManagement().getClientId();
        String clientSecret = auth0Properties.getManagement().getClientSecret();

        logger.info("Obtendo token de acesso da Auth0 Management API para o domínio {}", domain);

        AuthAPI authAPI = new AuthAPI(domain, clientId, clientSecret);
        TokenHolder holder = authAPI.requestToken("https://" + domain + "/api/v2/").execute().getBody();

        return new ManagementAPI(domain, holder.getAccessToken());
    }
}
