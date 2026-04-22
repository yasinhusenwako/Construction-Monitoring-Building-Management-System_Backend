
package com.org.cmbms.auth.service;

import com.org.cmbms.auth.dto.AuthResponse;
import com.org.cmbms.auth.dto.LoginRequest;
import com.org.cmbms.auth.dto.RegisterRequest;
import com.org.cmbms.auth.jwt.JwtUtil;
import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException("Email already exists");
        }
        Role role = request.getRole() == null ? Role.USER : request.getRole();
        if (role == Role.SUPERVISOR && request.getDivisionId() == null) {
            throw new ApiException("divisionId is required for supervisor");
        }
        if (role == Role.SUPERVISOR) {
            DivisionRules.assertAllowed(request.getDivisionId());
        }
        if (role == Role.SUPERVISOR && !userRepository.findByRoleAndDivisionId(Role.SUPERVISOR, request.getDivisionId()).isEmpty()) {
            throw new ApiException("A supervisor account already exists for this division");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setProfession(role == Role.PROFESSIONAL ? request.getProfession() : null);
        if (role == Role.SUPERVISOR) {
            user.setDivisionId(request.getDivisionId());
        } else {
            user.setDivisionId(null);
        }
        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(new UserPrincipal(saved));
        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.getDivisionId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(new UserPrincipal(user));
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getDivisionId());
    }

    public String forgotPassword(String email) {
        userRepository.findByEmail(email).orElseThrow(() -> new ApiException("User not found"));
        return "Password reset request accepted";
    }
}
