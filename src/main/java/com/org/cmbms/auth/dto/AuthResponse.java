package com.org.cmbms.auth.dto;

import com.org.cmbms.common.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long divisionId;

    @JsonProperty("fullName")
    public String getFullName() {
        return name;
    }
}

