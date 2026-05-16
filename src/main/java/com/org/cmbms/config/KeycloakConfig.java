package com.org.cmbms.config;

import lombok.extern.slf4j.Slf4j;
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

        try {
            // Check if we have username/password (admin credentials)
            if (username != null && !username.isEmpty() && 
                password != null && !password.isEmpty() && 
                !"admin".equals(username)) {
                
                log.info("Using admin username/password authentication");
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)
                        .realm("master")
                        .clientId("admin-cli")
                        .username(username)
                        .password(password)
                        .grantType("password")
                        .build();
                
                log.info("Keycloak Admin Client initialized with password grant");
                return keycloak;
            }
            
            // Otherwise use client credentials (service account)
            if (clientSecret == null || clientSecret.isEmpty()) {
                throw new IllegalStateException("Keycloak client secret is not configured. " +
                    "Please set KEYCLOAK_ADMIN_CLIENT_SECRET or provide admin username/password.");
            }
            
            log.info("Using client credentials (service account) authentication");
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .grantType("client_credentials")
                    .build();

            log.info("Keycloak Admin Client initialized with client credentials");
            return keycloak;
        } catch (Exception e) {
            log.error("Failed to initialize Keycloak Admin Client", e);
            throw e;
        }
    }
}
