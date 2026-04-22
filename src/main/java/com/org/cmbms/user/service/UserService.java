
package com.org.cmbms.user.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
import com.org.cmbms.user.dto.UserUpdateRequest;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> allUsers(UserPrincipal currentUser) {
        // Allow all authenticated users to fetch users for assignment dropdowns
        return userRepository.findAll();
    }

    public User currentUser(UserPrincipal currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException("User not found"));
    }

    public User updateUser(Long userId, UserUpdateRequest request, UserPrincipal currentUser) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new ApiException("Only admins can update users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        Role nextRole = request.getRole() == null ? user.getRole() : request.getRole();
        Long nextDivisionId = request.getDivisionId();

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
}
