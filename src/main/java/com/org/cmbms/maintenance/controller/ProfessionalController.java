package com.org.cmbms.maintenance.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.file.model.FileRecord;
import com.org.cmbms.file.service.FileStorageService;
import com.org.cmbms.maintenance.dto.TaskStatusUpdateRequest;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.service.WorkflowService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/professional")
@RequiredArgsConstructor
@Validated
public class ProfessionalController {

    private final WorkflowService workflowService;
    private final FileStorageService fileStorageService;

    @GetMapping("/tasks")
    public ResponseEntity<List<MaintenanceRequest>> tasks() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.getProfessionalTasks(user));
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<MaintenanceRequest> updateStatus(@PathVariable Long id, @jakarta.validation.Valid @RequestBody TaskStatusUpdateRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.updateTaskStatus(user, id, request));
    }

    @PostMapping("/upload-proof")
    public ResponseEntity<Map<String, Object>> uploadProof(@RequestParam @NotNull Long maintenanceRequestId,
                                                           @RequestParam("file") MultipartFile file) throws IOException {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        FileRecord record = fileStorageService.save(maintenanceRequestId, "MAINTENANCE_PROOF", file, user.getId());
        return ResponseEntity.ok(Map.of(
                "id", record.getId(),
                "fileName", record.getFileName(),
                "filePath", record.getFilePath()
        ));
    }
}

