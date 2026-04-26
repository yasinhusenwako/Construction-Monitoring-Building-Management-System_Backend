
package com.org.cmbms.maintenance.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.maintenance.dto.CreateMaintenanceRequestDTO;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceRequest> create(@Valid @RequestBody CreateMaintenanceRequestDTO request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(maintenanceService.create(request, user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MaintenanceRequest> update(@PathVariable Long id, @RequestBody CreateMaintenanceRequestDTO request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(maintenanceService.update(id, request, user));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRequest>> search(@RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String priority,
                                                           @RequestParam(required = false) String maintenanceId,
                                                           @RequestParam(required = false) Long divisionId,
                                                           @RequestParam(required = false) Long createdBy) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(maintenanceService.search(user, status, priority, maintenanceId, divisionId, createdBy));
    }

    @PostMapping("/{id}/upload-doc")
    public ResponseEntity<com.org.cmbms.maintenance.dto.MaintenanceDocUploadResponse> uploadDoc(@PathVariable("id") Long id,
                                                                                                @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        com.org.cmbms.file.model.FileRecord record = maintenanceService.uploadDoc(id, file, currentUser.getId());
        return ResponseEntity.ok(new com.org.cmbms.maintenance.dto.MaintenanceDocUploadResponse(record.getId(), record.getFileName(), record.getFilePath()));
    }
}
