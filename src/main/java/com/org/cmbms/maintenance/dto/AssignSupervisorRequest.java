package com.org.cmbms.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignSupervisorRequest {
    @NotNull
    @JsonAlias("maintenanceRequestId")
    private Long requestId;
    @NotNull
    private String divisionId; // Changed to String to support "DIV-001" format
    @NotNull
    private String supervisorId; // Changed to String to support both numeric IDs and email identifiers
    private String priority;
}

