package com.org.cmbms.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReviewRequest {
    @NotNull
    private Long requestId;
    @NotNull
    private String requestType;
}
