package com.org.cmbms.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class KeycloakConfig {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Bean
    public KeycloakSpringBootConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Value("${keycloak.admin.username:admin}")
    private String username;

    @Value("${keycloak.admin.password:admin}")
    private String password;

    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak Admin Client");
        log.info("Server URL: {}", serverUrl);
        log.info("Realm: {}", realm);
        log.info("Client ID: {}", clientId);
        log.info("Using service account authentication");
        
        try {
            // Use service account (client credentials) instead of username/password
            // This is more reliable and secure
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .grantType("client_credentials")
                    .build();
            
            log.info("Keycloak Admin Client initialized successfully");
            return keycloak;
        } catch (Exception e) {
            log.error("Failed to initialize Keycloak Admin Client", e);
            throw e;
        }
    }
}
