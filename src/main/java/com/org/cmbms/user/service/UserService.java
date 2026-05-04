
package com.org.cmbms.user.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
import com.org.cmbms.user.dto.UserUpdateRequest;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public List<User> allUsers(UserPrincipal currentUser) {
        // Get database users
        List<User> dbUsers = userRepository.findAll();
        
        // Get Keycloak users and convert to User objects
        try {
            List<UserRepresentation> keycloakUsers = keycloakAdminService.getAllUsers();
            List<User> allUsers = new ArrayList<>(dbUsers);
            
            for (UserRepresentation kcUser : keycloakUsers) {
                // Convert Keycloak user to User object
                User user = new User();
                
                String email = kcUser.getEmail() != null ? kcUser.getEmail() : kcUser.getUsername();
                
                // IMPORTANT: For Keycloak users, we use a special ID format that the frontend can recognize
                // Format: "KC:<email>" - this tells the frontend to use the email as the professional ID
                user.setId(-1L); // Negative ID indicates Keycloak user
                user.setEmail(email);
                user.setPassword(""); // Keycloak users don't have passwords in our database
                
                // Set name
                if (kcUser.getFirstName() != null && kcUser.getLastName() != null) {
                    user.setName(kcUser.getFirstName() + " " + kcUser.getLastName());
                } else {
                    user.setName(kcUser.getUsername());
                }
                
                // Get role from Keycloak roles
                List<String> roles = keycloakAdminService.getUserRoles(kcUser.getId());
                if (roles.contains("ADMIN")) {
                    user.setRole(Role.ADMIN);
                } else if (roles.contains("SUPERVISOR")) {
                    user.setRole(Role.SUPERVISOR);
                } else if (roles.contains("PROFESSIONAL")) {
                    user.setRole(Role.PROFESSIONAL);
                } else {
                    user.setRole(Role.USER);
                }
                
                // Get custom attributes
                Map<String, List<String>> attributes = kcUser.getAttributes();
                if (attributes != null) {
                    // Get divisionId
                    List<String> divisionIds = attributes.get("divisionId");
                    if (divisionIds != null && !divisionIds.isEmpty()) {
                        user.setDivisionId(divisionIds.get(0));
                    }
                    
                    // Get phone
                    List<String> phones = attributes.get("phone");
                    if (phones != null && !phones.isEmpty()) {
                        user.setPhone(phones.get(0));
                    }
                    
                    // Get department
                    List<String> departments = attributes.get("department");
                    if (departments != null && !departments.isEmpty()) {
                        user.setDepartment(departments.get(0));
                    }
                    
                    // Get profession
                    List<String> professions = attributes.get("profession");
                    if (professions != null && !professions.isEmpty()) {
                        user.setProfession(professions.get(0));
                    }
                }
                
                allUsers.add(user);
            }
            
            return allUsers;
        } catch (Exception e) {
            // If Keycloak is not available, just return database users
            System.err.println("Failed to fetch Keycloak users: " + e.getMessage());
            return dbUsers;
        }
    }

    public User currentUser(UserPrincipal currentUser) {
        // Try to parse ID as Long for database users
        try {
            Long numericId = Long.parseLong(currentUser.getId());
            return userRepository.findById(numericId)
                    .orElseThrow(() -> new ApiException("User not found"));
        } catch (NumberFormatException e) {
            // Keycloak user - return a virtual user object
            User virtualUser = new User();
            virtualUser.setId(0L); // Placeholder ID
            virtualUser.setName(currentUser.getName());
            virtualUser.setEmail(currentUser.getEmail());
            virtualUser.setRole(currentUser.getRole());
            virtualUser.setDivisionId(currentUser.getDivisionId());
            return virtualUser;
        }
    }

    public User updateUser(Long userId, UserUpdateRequest request, UserPrincipal currentUser) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can update users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        Role nextRole = request.getRole() == null ? user.getRole() : request.getRole();
        String nextDivisionId = request.getDivisionId();

        if (nextRole == Role.SUPERVISOR) {
            if (nextDivisionId == null) {
                throw new ApiException("divisionId is required for supervisor");
            }
            DivisionRules.assertAllowed(nextDivisionId);
            List<User> existingSupervisors = userRepository.findByRoleAndDivisionId(Role.SUPERVISOR, nextDivisionId);
            boolean hasOtherSupervisor = existingSupervisors.stream().anyMatch(u -> !u.getId().equals(userId));
            if (hasOtherSupervisor) {
                throw new ApiException("A supervisor account already exists for this division");
            }
        } else if (nextRole == Role.PROFESSIONAL) {
            if (nextDivisionId != null) {
                DivisionRules.assertAllowed(nextDivisionId);
            }
            String profession = request.getProfession() == null ? "" : request.getProfession().trim();
            if (profession.isEmpty()) {
                throw new ApiException("profession is required for professional");
            }
        } else {
            nextDivisionId = null;
        }

        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setRole(nextRole);
        user.setDivisionId(nextDivisionId);
        user.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        user.setDepartment(request.getDepartment() == null ? null : request.getDepartment().trim());
        user.setProfession(nextRole == Role.PROFESSIONAL
                ? (request.getProfession() == null ? null : request.getProfession().trim())
                : null);

        return userRepository.save(user);
    }

    public void deleteUser(Long userId, UserPrincipal currentUser) {
        // Only admins can delete users
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can delete users");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        // Prevent deleting the last admin
        if (user.getRole().equals(Role.ADMIN) && userRepository.findByRole(Role.ADMIN).size() <= 1) {
            throw new ApiException("Cannot delete the last admin user");
        }
        userRepository.delete(user);
    }

    /**
     * Get professionals filtered by division ID
     * divisionId="0" returns admin professionals (for projects/bookings)
     * divisionId="1","2","3" returns division professionals (for maintenance)
     */
    public List<User> getProfessionalsByDivision(String divisionId, UserPrincipal currentUser) {
        // Get all users (including Keycloak users)
        List<User> allUsers = allUsers(currentUser);
        
        // Filter for professionals
        List<User> professionals = allUsers.stream()
                .filter(user -> user.getRole() == Role.PROFESSIONAL)
                .toList();
        
        // If divisionId is provided, filter by division
        if (divisionId != null && !divisionId.isEmpty()) {
            return professionals.stream()
                    .filter(user -> divisionId.equals(user.getDivisionId()))
                    .toList();
        }
        
        // Return all professionals if no divisionId specified
        return professionals;
    }

    /**
     * Get all professionals (admin only)
     */
    public List<User> getAllProfessionals(UserPrincipal currentUser) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can view all professionals");
        }
        
        // Get all users (including Keycloak users)
        List<User> allUsers = allUsers(currentUser);
        
        // Filter for professionals
        return allUsers.stream()
                .filter(user -> user.getRole() == Role.PROFESSIONAL)
                .toList();
    }
}
