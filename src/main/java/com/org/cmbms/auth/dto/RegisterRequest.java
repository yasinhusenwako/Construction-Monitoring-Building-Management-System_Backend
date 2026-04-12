package com.org.cmbms.auth.dto;

import com.org.cmbms.common.enums.Role;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    @JsonAlias("fullName")
    private String name;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private Role role;
    private Long divisionId;
}

