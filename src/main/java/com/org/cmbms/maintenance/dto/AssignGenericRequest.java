package com.org.cmbms.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignGenericRequest {
    @NotNull
    @JsonAlias("maintenanceRequestId")
    private Long requestId;
}

