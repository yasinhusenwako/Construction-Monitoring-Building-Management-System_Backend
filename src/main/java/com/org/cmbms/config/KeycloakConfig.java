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
        log.info("Admin Username: {}", username);
        log.info("Using master realm admin credentials");

        try {
            // Use master realm admin credentials (password grant) for full admin access
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm("master")
                    .clientId("admin-cli")
                    .username(username)
                    .password(password)
                    .grantType("password")
                    .build();

            log.info("Keycloak Admin Client initialized successfully");
            return keycloak;
        } catch (Exception e) {
            log.error("Failed to initialize Keycloak Admin Client", e);
            throw e;
        }
    }
}
