package com.org.cmbms.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoqRequest {
    @NotBlank
    private String projectId;
}

