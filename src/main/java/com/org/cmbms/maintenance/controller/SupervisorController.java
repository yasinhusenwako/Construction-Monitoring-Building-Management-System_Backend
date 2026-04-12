package com.org.cmbms.maintenance.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.maintenance.dto.AssignProfessionalRequest;
import com.org.cmbms.maintenance.dto.ReviewRequest;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supervisor")
@RequiredArgsConstructor
public class SupervisorController {

    private final WorkflowService workflowService;

    @GetMapping("/requests")
    public ResponseEntity<List<MaintenanceRequest>> requests() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.getSupervisorRequests(user));
    }

    @PostMapping("/assign-professional")
    public ResponseEntity<MaintenanceRequest> assignProfessional(@Valid @RequestBody AssignProfessionalRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.assignProfessional(user, request));
    }

    @PostMapping("/review")
    public ResponseEntity<MaintenanceRequest> review(@Valid @RequestBody ReviewRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.supervisorReview(user, request.getRequestId()));
    }
}

