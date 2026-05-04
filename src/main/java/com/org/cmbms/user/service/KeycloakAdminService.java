package com.org.cmbms.user.service;

import com.org.cmbms.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    /**
     * Get all users from Keycloak
     */
    public List<UserRepresentation> getAllUsers() {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();
            return usersResource.list();
        } catch (Exception e) {
            log.error("Failed to fetch users from Keycloak", e);
            throw new ApiException("Failed to fetch users from Keycloak: " + e.getMessage());
        }
    }

    /**
     * Get a single user by ID
     */
    public UserRepresentation getUserById(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            return userResource.toRepresentation();
        } catch (Exception e) {
            log.error("Failed to fetch user from Keycloak", e);
            throw new ApiException("User not found in Keycloak");
        }
    }

    /**
     * Create a new user in Keycloak
     */
    public String createUser(String username, String email, String firstName, String lastName, 
                            String password, List<String> roles) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(username, true);
            if (!existingUsers.isEmpty()) {
                throw new ApiException("User with username '" + username + "' already exists");
            }

            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Create user
            Response response = usersResource.create(user);
            
            if (response.getStatus() != 201) {
                throw new ApiException("Failed to create user in Keycloak: " + response.getStatusInfo());
            }

            // Extract user ID from location header
            String locationHeader = response.getHeaderString("Location");
            String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            response.close();

            // Set password
            if (password != null && !password.isEmpty()) {
                setUserPassword(userId, password, false);
            }

            // Assign roles
            if (roles != null && !roles.isEmpty()) {
                assignRolesToUser(userId, roles);
            }

            log.info("Successfully created user in Keycloak: {}", username);
            return userId;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak", e);
            throw new ApiException("Failed to create user in Keycloak: " + e.getMessage());
        }
    }

    /**
     * Update an existing user in Keycloak
     */
    public void updateUser(String userId, String email, String firstName, String lastName, 
                          Boolean enabled, List<String> roles) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Update user fields
            if (email != null) user.setEmail(email);
            if (firstName != null) user.setFirstName(firstName);
            if (lastName != null) user.setLastName(lastName);
            if (enabled != null) user.setEnabled(enabled);

            userResource.update(user);

            // Update roles if provided
            if (roles != null) {
                // Remove all existing realm roles first
                List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listEffective();
                List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                    .filter(role -> !role.getName().startsWith("default-") && 
                                   !role.getName().equals("offline_access") &&
                                   !role.getName().equals("uma_authorization"))
                    .collect(Collectors.toList());
                
                if (!rolesToRemove.isEmpty()) {
                    userResource.roles().realmLevel().remove(rolesToRemove);
                }

                // Assign new roles
                assignRolesToUser(userId, roles);
            }

            log.info("Successfully updated user in Keycloak: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user in Keycloak", e);
            throw new ApiException("Failed to update user in Keycloak: " + e.getMessage());
        }
    }

    /**
     * Delete a user from Keycloak
     */
    public void deleteUser(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            
            // Check if user exists
            userResource.toRepresentation();
            
            // Delete user
            userResource.remove();
            
            log.info("Successfully deleted user from Keycloak: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user from Keycloak", e);
            throw new ApiException("Failed to delete user from Keycloak: " + e.getMessage());
        }
    }

    /**
     * Set user password
     */
    public void setUserPassword(String userId, String password, boolean temporary) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporary);

            userResource.resetPassword(credential);
            log.info("Successfully set password for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to set password for user", e);
            throw new ApiException("Failed to set password: " + e.getMessage());
        }
    }

    /**
     * Assign roles to a user
     */
    public void assignRolesToUser(String userId, List<String> roleNames) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            List<RoleRepresentation> roles = new ArrayList<>();
            for (String roleName : roleNames) {
                RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                if (role != null) {
                    roles.add(role);
                }
            }

            if (!roles.isEmpty()) {
                userResource.roles().realmLevel().add(roles);
                log.info("Successfully assigned roles to user {}: {}", userId, roleNames);
            }
        } catch (Exception e) {
            log.error("Failed to assign roles to user", e);
            throw new ApiException("Failed to assign roles: " + e.getMessage());
        }
    }

    /**
     * Get user's realm roles
     */
    public List<String> getUserRoles(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listEffective();
            return roles.stream()
                .map(RoleRepresentation::getName)
                .filter(name -> !name.startsWith("default-") && 
                               !name.equals("offline_access") &&
                               !name.equals("uma_authorization"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get user roles", e);
            return Collections.emptyList();
        }
    }

    /**
     * Enable/Disable user
     */
    public void setUserEnabled(String userId, boolean enabled) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            
            user.setEnabled(enabled);
            userResource.update(user);
            
            log.info("Successfully {} user: {}", enabled ? "enabled" : "disabled", userId);
        } catch (Exception e) {
            log.error("Failed to update user status", e);
            throw new ApiException("Failed to update user status: " + e.getMessage());
        }
    }

    /**
     * Get all available realm roles
     */
    public List<String> getAvailableRoles() {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            List<RoleRepresentation> roles = realmResource.roles().list();
            
            return roles.stream()
                .map(RoleRepresentation::getName)
                .filter(name -> !name.startsWith("default-") && 
                               !name.equals("offline_access") &&
                               !name.equals("uma_authorization"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get available roles", e);
            return Arrays.asList("ADMIN", "SUPERVISOR", "PROFESSIONAL", "USER");
        }
    }

    /**
     * Get users by role from Keycloak
     */
    public List<UserRepresentation> getUsersByRole(String roleName) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            
            // Get the role
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            if (role == null) {
                log.warn("Role not found in Keycloak: {}", roleName);
                return Collections.emptyList();
            }
            
            // Get all users with this role
            List<UserRepresentation> usersWithRole = realmResource.roles()
                .get(roleName)
                .getUserMembers();
            
            return usersWithRole != null ? usersWithRole : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get users by role from Keycloak: {}", roleName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Update user custom attributes
     */
    public void updateUserAttributes(String userId, Map<String, List<String>> attributes) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            
            user.setAttributes(attributes);
            userResource.update(user);
            
            log.info("Successfully updated user attributes for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user attributes", e);
            throw new ApiException("Failed to update user attributes: " + e.getMessage());
        }
    }
}
