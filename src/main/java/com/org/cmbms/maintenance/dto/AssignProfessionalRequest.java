package com.org.cmbms.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignProfessionalRequest {
    @NotNull
    @JsonAlias("maintenanceRequestId")
    private Long requestId;
    @NotNull
    @JsonAlias("professionalId")
    private String assignedProfessionalId; // Changed to String to support both numeric IDs and email identifiers
    @NotBlank
    private String instructions;
}

