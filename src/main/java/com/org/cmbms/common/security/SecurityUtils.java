package com.org.cmbms.common.security;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    /**
     * Get current user from either Keycloak JWT token or traditional UserPrincipal
     * This method supports both authentication systems
     */
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new ApiException("Unauthorized");
        }

        // Check if it's a Keycloak JWT token
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return extractUserPrincipalFromJwt(jwtAuth);
        }
        
        // Check if it's traditional UserPrincipal
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        
        throw new ApiException("Unauthorized");
    }
    
    // Store KeycloakAdminService instance for fallback divisionId lookup
    private static com.org.cmbms.user.service.KeycloakAdminService keycloakAdminService;
    
    /**
     * Set KeycloakAdminService for fallback divisionId lookup
     * This is called by Spring during initialization
     */
    public static void setKeycloakAdminService(com.org.cmbms.user.service.KeycloakAdminService service) {
        keycloakAdminService = service;
    }

    /**
     * Extract UserPrincipal from Keycloak JWT token
     */
    private static UserPrincipal extractUserPrincipalFromJwt(JwtAuthenticationToken jwtAuth) {
        Jwt jwt = jwtAuth.getToken();
        
        // Extract user information from JWT claims
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String subject = jwt.getSubject(); // Keycloak user ID
        
        // Extract role from granted authorities
        Role role = extractRoleFromAuthorities(jwtAuth.getAuthorities());
        
        // Extract divisionId from JWT token attributes
        String divisionId = null;
        try {
            // Try to get divisionId from direct claims first
            divisionId = jwt.getClaimAsString("divisionId");
            
            // If not found, try alternative claim names
            if (divisionId == null || divisionId.isBlank()) {
                divisionId = jwt.getClaimAsString("division_id");
            }
            if (divisionId == null || divisionId.isBlank()) {
                divisionId = jwt.getClaimAsString("divisionid");
            }
            
            // If still not found, try to extract from attributes object
            if (divisionId == null || divisionId.isBlank()) {
                Object attributesObj = jwt.getClaim("attributes");
                if (attributesObj instanceof java.util.Map) {
                    java.util.Map<String, Object> attributes = (java.util.Map<String, Object>) attributesObj;
                    Object divIdObj = attributes.get("divisionId");
                    if (divIdObj != null) {
                        if (divIdObj instanceof java.util.List) {
                            java.util.List<?> divIdList = (java.util.List<?>) divIdObj;
                            if (!divIdList.isEmpty()) {
                                divisionId = String.valueOf(divIdList.get(0));
                            }
                        } else {
                            divisionId = String.valueOf(divIdObj);
                        }
                    }
                }
            }
            
            // FALLBACK: If divisionId is still not found and user is SUPERVISOR or PROFESSIONAL,
            // fetch it from Keycloak user attributes
            if ((divisionId == null || divisionId.isBlank()) && 
                (role == Role.SUPERVISOR || role == Role.PROFESSIONAL) &&
                keycloakAdminService != null && subject != null) {
                
                System.out.println("⚠️ divisionId not in token, fetching from Keycloak for user: " + subject);
                try {
                    org.keycloak.representations.idm.UserRepresentation keycloakUser = 
                        keycloakAdminService.getUserById(subject);
                    
                    if (keycloakUser != null && keycloakUser.getAttributes() != null) {
                        java.util.List<String> divisionIdList = keycloakUser.getAttributes().get("divisionId");
                        if (divisionIdList != null && !divisionIdList.isEmpty()) {
                            divisionId = divisionIdList.get(0);
                            System.out.println("✅ Fetched divisionId from Keycloak: " + divisionId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Failed to fetch divisionId from Keycloak: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Division ID extracted from token: " + divisionId);
        } catch (Exception e) {
            System.err.println("❌ Failed to extract divisionId from JWT token: " + e.getMessage());
            e.printStackTrace();
        }
        
        // For Keycloak users, use email as the identifier
        String userIdentifier = email != null ? email : preferredUsername;
        
        // Create UserPrincipal with divisionId from Keycloak attributes
        return new UserPrincipal(
                userIdentifier,
                name != null ? name : preferredUsername,
                email != null ? email : preferredUsername,
                role,
                divisionId
        );
    }

    /**
     * Extract the highest priority role from granted authorities
     */
    private static Role extractRoleFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        // Priority order: ADMIN > SUPERVISOR > PROFESSIONAL > USER
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority().replace("ROLE_", "");
            
            if ("ADMIN".equals(role)) {
                return Role.ADMIN;
            }
        }
        
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority().replace("ROLE_", "");
            
            if ("SUPERVISOR".equals(role)) {
                return Role.SUPERVISOR;
            }
        }
        
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority().replace("ROLE_", "");
            
            if ("PROFESSIONAL".equals(role)) {
                return Role.PROFESSIONAL;
            }
        }
        
        // Default to USER if no specific role found
        return Role.USER;
    }
}

