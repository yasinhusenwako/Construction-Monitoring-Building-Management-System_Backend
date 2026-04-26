package com.org.cmbms.maintenance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaintenanceDocUploadResponse {
    private Long id;
    private String fileName;
    private String filePath;
}
