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
    private Long divisionId;
    @NotNull
    private Long supervisorId;
    private String priority;
}

