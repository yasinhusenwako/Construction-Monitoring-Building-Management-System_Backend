package com.org.cmbms.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMaintenanceRequestDTO {
    @NotBlank
    private String maintenanceId;
    @NotBlank
    private String category;
    @NotBlank
    private String priority;
    @NotBlank
    private String description;
    @NotBlank
    private String location;
    private Long divisionId;
}

