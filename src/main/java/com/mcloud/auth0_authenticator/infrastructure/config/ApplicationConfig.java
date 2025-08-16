package com.mcloud.auth0_authenticator.infrastructure.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;


@Configuration
public class ApplicationConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(new Locale("pt", "BR")); // Força o locale pt_BR
        messageSource.setUseCodeAsDefaultMessage(false); // Evita usar o código como mensagem padrão
        messageSource.setFallbackToSystemLocale(false); // Desativa fallback para o locale do sistema
        return messageSource;
    }

}