package com.org.cmbms.user.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.user.dto.KeycloakUserCreateRequest;
import com.org.cmbms.user.dto.KeycloakUserDTO;
import com.org.cmbms.user.dto.KeycloakUserUpdateRequest;
import com.org.cmbms.user.service.KeycloakAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/keycloak/users")
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserController {

    private final KeycloakAdminService keycloakAdminService;

    /**
     * Get all users from Keycloak
     */
    @GetMapping
    public ResponseEntity<List<KeycloakUserDTO>> getAllUsers() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can view all users
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can view all users");
        }

        List<UserRepresentation> keycloakUsers = keycloakAdminService.getAllUsers();
        List<KeycloakUserDTO> users = keycloakUsers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * Get a single user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<KeycloakUserDTO> getUserById(@PathVariable String userId) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can view user details
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can view user details");
        }

        UserRepresentation keycloakUser = keycloakAdminService.getUserById(userId);
        KeycloakUserDTO user = convertToDTO(keycloakUser);

        return ResponseEntity.ok(user);
    }

    /**
     * Create a new user in Keycloak
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(@Valid @RequestBody KeycloakUserCreateRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can create users
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can create users");
        }

        log.info("Creating user in Keycloak: {}", request.getUsername());

        String userId = keycloakAdminService.createUser(
            request.getUsername(),
            request.getEmail(),
            request.getFirstName(),
            request.getLastName(),
            request.getPassword(),
            request.getRoles(),
            true // enabled by default
        );

        // Always update user attributes (even if empty, to ensure consistency)
        updateUserAttributes(userId, request.getPhone(), request.getDepartment(), 
                           request.getDivisionId(), request.getProfession());

        Map<String, String> response = new HashMap<>();
        response.put("id", userId);
        response.put("message", "User created successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing user in Keycloak
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, String>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody KeycloakUserUpdateRequest request) {
        
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can update users
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can update users");
        }

        log.info("Updating user in Keycloak: {}", userId);

        keycloakAdminService.updateUser(
            userId,
            request.getEmail(), // username - using email as username
            request.getEmail(),
            request.getFirstName(),
            request.getLastName(),
            request.getEnabled(),
            request.getRoles()
        );

        // Always update user attributes (to handle clearing attributes)
        updateUserAttributes(userId, request.getPhone(), request.getDepartment(), 
                           request.getDivisionId(), request.getProfession());

        Map<String, String> response = new HashMap<>();
        response.put("message", "User updated successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a user from Keycloak
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can delete users
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can delete users");
        }

        log.info("Deleting user from Keycloak: {}", userId);

        // Check if user has ADMIN role - prevent deleting last admin
        List<String> userRoles = keycloakAdminService.getUserRoles(userId);
        if (userRoles.contains("ADMIN")) {
            // Count total admins
            List<UserRepresentation> allUsers = keycloakAdminService.getAllUsers();
            long adminCount = allUsers.stream()
                .filter(u -> {
                    List<String> roles = keycloakAdminService.getUserRoles(u.getId());
                    return roles.contains("ADMIN");
                })
                .count();
            
            if (adminCount <= 1) {
                throw new ApiException("Cannot delete the last admin user");
            }
        }

        keycloakAdminService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Enable/Disable a user
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<Map<String, String>> toggleUserStatus(
            @PathVariable String userId,
            @RequestParam boolean enabled) {
        
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can change user status
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can change user status");
        }

        log.info("{} user in Keycloak: {}", enabled ? "Enabling" : "Disabling", userId);

        keycloakAdminService.setUserEnabled(userId, enabled);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User status updated successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Reset user password
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can reset passwords
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can reset passwords");
        }

        String newPassword = request.get("password");
        boolean temporary = Boolean.parseBoolean(request.getOrDefault("temporary", "false"));

        if (newPassword == null || newPassword.isEmpty()) {
            throw new ApiException("Password is required");
        }

        log.info("Resetting password for user: {}", userId);

        keycloakAdminService.setUserPassword(userId, newPassword, temporary);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get available roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAvailableRoles() {
        List<String> roles = keycloakAdminService.getAvailableRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Convert Keycloak UserRepresentation to DTO
     */
    private KeycloakUserDTO convertToDTO(UserRepresentation user) {
        List<String> roles = keycloakAdminService.getUserRoles(user.getId());
        
        // Get custom attributes
        Map<String, List<String>> attributes = user.getAttributes() != null ? 
            user.getAttributes() : new HashMap<>();
        
        String phone = getFirstAttribute(attributes, "phone");
        String department = getFirstAttribute(attributes, "department");
        String divisionId = getFirstAttribute(attributes, "divisionId");
        String profession = getFirstAttribute(attributes, "profession");
        
        // Determine status based on enabled flag
        String status = user.isEnabled() ? "active" : "inactive";
        
        // Generate avatar from name
        String avatar = "";
        if (user.getFirstName() != null && user.getLastName() != null) {
            avatar = (user.getFirstName().substring(0, 1) + user.getLastName().substring(0, 1)).toUpperCase();
        } else if (user.getUsername() != null && user.getUsername().length() >= 2) {
            avatar = user.getUsername().substring(0, 2).toUpperCase();
        }
        
        return KeycloakUserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .enabled(user.isEnabled())
            .emailVerified(user.isEmailVerified())
            .createdTimestamp(user.getCreatedTimestamp())
            .roles(roles)
            .attributes(attributes)
            .phone(phone)
            .department(department)
            .divisionId(divisionId)
            .profession(profession)
            .status(status)
            .avatar(avatar)
            .build();
    }

    /**
     * Get first attribute value from attributes map
     */
    private String getFirstAttribute(Map<String, List<String>> attributes, String key) {
        List<String> values = attributes.get(key);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    /**
     * Update user custom attributes
     */
    private void updateUserAttributes(String userId, String phone, String department, 
                                     String divisionId, String profession) {
        try {
            log.info("Updating user attributes for userId: {}", userId);
            log.info("Phone: {}, Department: {}, DivisionId: {}, Profession: {}", 
                    phone, department, divisionId, profession);
            
            UserRepresentation user = keycloakAdminService.getUserById(userId);
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            // Update or remove attributes based on whether they're provided and non-empty
            if (phone != null && !phone.trim().isEmpty()) {
                attributes.put("phone", List.of(phone.trim()));
            } else if (phone != null && phone.trim().isEmpty()) {
                attributes.remove("phone");
            }
            
            if (department != null && !department.trim().isEmpty()) {
                attributes.put("department", List.of(department.trim()));
            } else if (department != null && department.trim().isEmpty()) {
                attributes.remove("department");
            }
            
            if (divisionId != null && !divisionId.trim().isEmpty()) {
                attributes.put("divisionId", List.of(divisionId.trim()));
            } else if (divisionId != null && divisionId.trim().isEmpty()) {
                attributes.remove("divisionId");
            }
            
            if (profession != null && !profession.trim().isEmpty()) {
                attributes.put("profession", List.of(profession.trim()));
            } else if (profession != null && profession.trim().isEmpty()) {
                attributes.remove("profession");
            }

            user.setAttributes(attributes);
            
            // Update user with new attributes using Keycloak API directly
            keycloakAdminService.updateUserAttributes(userId, attributes);
            
            log.info("Successfully updated user attributes for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user attributes", e);
            throw new ApiException("Failed to update user attributes: " + e.getMessage());
        }
    }
}
