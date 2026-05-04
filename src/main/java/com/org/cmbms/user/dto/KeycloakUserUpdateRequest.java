package com.org.cmbms.user.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserUpdateRequest {
    
    @Email(message = "Email must be valid")
    private String email;
    
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private List<String> roles;
    
    // Additional fields
    private String phone;
    private String department;
    private String divisionId;
    private String profession;
}
