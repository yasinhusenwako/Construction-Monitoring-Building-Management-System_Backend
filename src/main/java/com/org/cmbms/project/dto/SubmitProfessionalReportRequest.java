package com.org.cmbms.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitProfessionalReportRequest {
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotBlank(message = "Report text is required")
    private String reportText;
}
