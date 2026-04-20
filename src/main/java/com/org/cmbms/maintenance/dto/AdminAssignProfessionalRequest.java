package com.org.cmbms.maintenance.dto;

import com.org.cmbms.common.enums.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAssignProfessionalRequest {
    @NotNull
    private Long requestId;
    @NotNull
    private RequestType requestType;
    @NotNull
    private Long assignedProfessionalId;
    private String instructions;
}
