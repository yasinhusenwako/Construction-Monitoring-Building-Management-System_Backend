
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

    @GetMapping
    public ResponseEntity<List<MaintenanceRequest>> search(@RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String priority,
                                                           @RequestParam(required = false) String maintenanceId,
                                                           @RequestParam(required = false) Long divisionId,
                                                           @RequestParam(required = false) Long createdBy) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(maintenanceService.search(user, status, priority, maintenanceId, divisionId, createdBy));
    }

    @PostMapping("/{id}/assign-professional")
    public ResponseEntity<MaintenanceRequest> assignProfessional(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        Long professionalId = Long.valueOf(body.get("professionalId").toString());
        String instructions = body.get("instructions") != null ? body.get("instructions").toString() : "";
        return ResponseEntity.ok(maintenanceService.adminAssignProfessional(id, professionalId, instructions, user));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<MaintenanceRequest> approve(@PathVariable Long id) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(maintenanceService.adminApprove(id, user));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<MaintenanceRequest> reject(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        String reason = body.get("reason");
        return ResponseEntity.ok(maintenanceService.adminReject(id, reason, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRequest> update(@PathVariable Long id, @Valid @RequestBody CreateMaintenanceRequestDTO request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(maintenanceService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        maintenanceService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
