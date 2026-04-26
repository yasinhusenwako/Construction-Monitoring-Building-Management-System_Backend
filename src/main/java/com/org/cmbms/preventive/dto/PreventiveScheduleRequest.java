package com.org.cmbms.preventive.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PreventiveScheduleRequest {
    private String system;
    private String frequency;
    private LocalDate lastDone;
    private LocalDate nextDue;
    private Long assignedProfessionalId;
    private String notes;
}
