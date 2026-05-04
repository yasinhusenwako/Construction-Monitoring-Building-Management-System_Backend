package com.org.cmbms.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAssignRequest {
    @NotNull
    private Long requestId;
    @NotNull
    private String requestType;
    @NotNull
    private String divisionId; // Changed to String to support "DIV-001" format
    private String supervisorId; // Changed to String to support both numeric IDs and email identifiers
    private String priority;
}

