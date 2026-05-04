package com.org.cmbms.auth.security;

import com.org.cmbms.common.enums.Role;
import com.org.cmbms.user.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final String id; // Changed to String to support both numeric IDs and email identifiers
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final String divisionId; // Changed to String to support "DIV-001" format

    public UserPrincipal(User user) {
        this.id = String.valueOf(user.getId());
        this.name = user.getName();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.divisionId = user.getDivisionId(); // Already String in User model
    }

    // Constructor for Keycloak users (without database User object)
    public UserPrincipal(String id, String name, String email, Role role, String divisionId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = ""; // Keycloak users don't have passwords in our system
        this.role = role;
        this.divisionId = divisionId;
    }
    
    // Helper method to get numeric ID (for backward compatibility with database users)
    public Long getNumericId() {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null; // Return null for Keycloak users (email-based IDs)
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

