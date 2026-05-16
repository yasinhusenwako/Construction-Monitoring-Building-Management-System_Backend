package com.org.cmbms.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class ProjectRequestDTO {
    @NotBlank
    private String projectId;
    @NotBlank
    private String title;
    @NotBlank
    private String location;
    private String block;
    private String floor;
    @NotBlank
    private String department;
    @NotBlank
    private String contactPerson;
    @NotBlank
    private String phone;
    @NotBlank
    private String siteCondition;
    @NotBlank
    private String description;
    @NotNull
    private BigDecimal budget;
    private LocalDate startDate; // Optional
    private LocalDate endDate; // Optional
    @NotBlank
    private String classification;
    @NotBlank
    private String priority;
    private String divisionId; // Changed to String to support "DIV-001" format
    private String requestMode;
    private String linkedProjectId;
    private Map<String, Object> scope;
}

