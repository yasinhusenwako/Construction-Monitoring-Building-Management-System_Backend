package com.org.cmbms.config;

import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.user.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration to inject KeycloakAdminService into SecurityUtils
 * This allows SecurityUtils to fetch divisionId from Keycloak when it's not in the JWT token
 */
@Configuration
@RequiredArgsConstructor
public class SecurityUtilsConfig {
    
    private final KeycloakAdminService keycloakAdminService;
    
    @PostConstruct
    public void init() {
        SecurityUtils.setKeycloakAdminService(keycloakAdminService);
    }
}
