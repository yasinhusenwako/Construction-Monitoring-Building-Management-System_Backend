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
    private Long divisionId;
    @NotNull
    private Long supervisorId;
    private String priority;
}

