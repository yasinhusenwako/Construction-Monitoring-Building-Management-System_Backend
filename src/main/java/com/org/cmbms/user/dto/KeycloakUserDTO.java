package com.org.cmbms.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserDTO {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Boolean emailVerified;
    private Long createdTimestamp;
    private List<String> roles;
    private Map<String, List<String>> attributes;
    
    // Additional fields for compatibility with frontend
    private String phone;
    private String department;
    private String divisionId;
    private String profession;
    private String status; // active, inactive, locked
    private String avatar;
}
