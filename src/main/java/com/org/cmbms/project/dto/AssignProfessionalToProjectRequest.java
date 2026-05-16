package com.org.cmbms.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignProfessionalToProjectRequest {
    
    // projectId comes from @PathVariable, not from request body
    private Long projectId;
    
    @NotBlank(message = "Professional ID is required")
    private String professionalId;
    
    @NotBlank(message = "Instructions/Scope is required")
    private String instructions;
}
