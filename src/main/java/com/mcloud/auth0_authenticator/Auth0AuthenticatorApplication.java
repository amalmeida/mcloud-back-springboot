package com.mcloud.auth0_authenticator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Auth0AuthenticatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(Auth0AuthenticatorApplication.class, args);
	}

}
