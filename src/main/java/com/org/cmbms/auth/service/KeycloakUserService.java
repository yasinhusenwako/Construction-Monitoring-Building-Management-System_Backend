package com.org.cmbms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloak;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public String createUser(String username, String email, String firstName, 
                            String lastName, String password, String role) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

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
            
            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath()
                        .replaceAll(".*/([^/]+)$", "$1");
                
                // Set password
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);
                
                usersResource.get(userId).resetPassword(credential);
                
                // Assign role
                assignRole(userId, role);
                
                log.info("User created successfully: {}", username);
                return userId;
            } else {
                log.error("Failed to create user. Status: {}", response.getStatus());
                throw new RuntimeException("Failed to create user");
            }
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            throw new RuntimeException("Error creating user", e);
        }
    }

    public void assignRole(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            realmResource.users().get(userId).roles().realmLevel()
                    .add(Collections.singletonList(
                            realmResource.roles().get(roleName).toRepresentation()
                    ));
            log.info("Role {} assigned to user {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error assigning role: {}", e.getMessage());
            throw new RuntimeException("Error assigning role", e);
        }
    }

    public void setUserAttribute(String userId, String attributeName, String attributeValue) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            user.setAttributes(Map.of(attributeName, List.of(attributeValue)));
            realmResource.users().get(userId).update(user);
            log.info("Attribute {} set for user {}", attributeName, userId);
        } catch (Exception e) {
            log.error("Error setting user attribute: {}", e.getMessage());
            throw new RuntimeException("Error setting user attribute", e);
        }
    }

    public List<UserRepresentation> getAllUsers() {
        return keycloak.realm(realm).users().list();
    }

    public UserRepresentation getUserByUsername(String username) {
        List<UserRepresentation> users = keycloak.realm(realm).users()
                .search(username, true);
        return users.isEmpty() ? null : users.get(0);
    }

    public UserRepresentation getUserById(String userId) {
        return keycloak.realm(realm).users().get(userId).toRepresentation();
    }

    public void deleteUser(String userId) {
        keycloak.realm(realm).users().delete(userId);
        log.info("User deleted: {}", userId);
    }

    public void updateUser(String userId, String email, String firstName, String lastName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            realmResource.users().get(userId).update(user);
            log.info("User updated: {}", userId);
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            throw new RuntimeException("Error updating user", e);
        }
    }

    public void resetPassword(String userId, String newPassword) {
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);
            
            keycloak.realm(realm).users().get(userId).resetPassword(credential);
            log.info("Password reset for user: {}", userId);
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage());
            throw new RuntimeException("Error resetting password", e);
        }
    }
}
