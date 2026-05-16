package com.org.cmbms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ProjectAssignmentDTO {
    private Long id;
    private Long projectId;
    private String professionalId;
    private String instructions;
    private LocalDateTime createdAt;
    private String createdBy;
    private String status;
}
