package com.org.cmbms.user.dto;

import com.org.cmbms.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    private Role role;
    private String divisionId; // Changed to String to support "DIV-001" format
    private String phone;
    private String department;
    private String profession;
}
