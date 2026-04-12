package com.org.cmbms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectDocUploadResponse {
    private Long id;
    private String fileName;
    private String filePath;
}

