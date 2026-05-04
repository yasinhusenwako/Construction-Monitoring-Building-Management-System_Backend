package com.org.cmbms.maintenance.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.maintenance.dto.AdminAssignRequest;
import com.org.cmbms.maintenance.dto.AssignGenericRequest;
import com.org.cmbms.maintenance.dto.AdminAssignProfessionalRequest;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.service.WorkflowService;
import com.org.cmbms.project.model.Project;
import com.org.cmbms.project.service.ProjectService;
import com.org.cmbms.space.model.Booking;
import com.org.cmbms.space.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WorkflowService workflowService;
    private final ProjectService projectService;
    private final SpaceService spaceService;

    @GetMapping("/all-requests")
    public ResponseEntity<List<MaintenanceRequest>> allRequests() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.getAllRequests(user));
    }

    @PatchMapping("/assign")
    public ResponseEntity<?> assign(@Valid @RequestBody AdminAssignRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        String type = request.getRequestType() == null ? "" : request.getRequestType().trim().toUpperCase();
        if ("MAINTENANCE".equals(type)) {
            MaintenanceRequest out = workflowService.assignSupervisor(user, map(request));
            return ResponseEntity.ok(out);
        }
        if ("PROJECT".equals(type)) {
            Project out = projectService.adminAssign(request.getRequestId(), request.getDivisionId(), request.getSupervisorId(), request.getPriority(), user);
            return ResponseEntity.ok(out);
        }
        if ("BOOKING".equals(type)) {
            Booking out = spaceService.adminAssign(request.getRequestId(), request.getDivisionId(), request.getSupervisorId(), user);
            return ResponseEntity.ok(out);
        }
        throw new com.org.cmbms.common.exception.ApiException("Unsupported requestType for this endpoint");
    }

    @PatchMapping("/assign-professional")
    public ResponseEntity<?> assignProfessional(@Valid @RequestBody AdminAssignProfessionalRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        RequestType type = request.getRequestType();
        String professionalId = request.getAssignedProfessionalId();
        
        if (RequestType.MAINTENANCE == type) {
            MaintenanceRequest out = workflowService.assignProfessional(user, new com.org.cmbms.maintenance.dto.AssignProfessionalRequest() {{
                setRequestId(request.getRequestId());
                setAssignedProfessionalId(professionalId);
                setInstructions(request.getInstructions());
            }});
            return ResponseEntity.ok(out);
        }
        if (RequestType.PROJECT == type) {
            Project out = projectService.adminAssignProfessional(request.getRequestId(), professionalId, request.getInstructions(), user);
            return ResponseEntity.ok(out);
        }
        if (RequestType.BOOKING == type) {
            Booking out = spaceService.adminAssignProfessional(request.getRequestId(), professionalId, request.getInstructions(), user);
            return ResponseEntity.ok(out);
        }
        throw new com.org.cmbms.common.exception.ApiException("Unsupported requestType for this endpoint");
    }

    @PatchMapping("/approve")
    public ResponseEntity<MaintenanceRequest> approve(@Valid @RequestBody AssignGenericRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.adminApprove(user, request.getRequestId()));
    }

    @PatchMapping("/reject")
    public ResponseEntity<MaintenanceRequest> reject(@Valid @RequestBody AssignGenericRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.adminReject(user, request.getRequestId()));
    }

    @PatchMapping("/close")
    public ResponseEntity<MaintenanceRequest> close(@Valid @RequestBody AssignGenericRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(workflowService.adminClose(user, request.getRequestId()));
    }

    @PatchMapping("/review")
    public ResponseEntity<?> review(@Valid @RequestBody com.org.cmbms.maintenance.dto.AdminReviewRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        String type = request.getRequestType() == null ? "" : request.getRequestType().trim().toUpperCase();
        try {
            if ("MAINTENANCE".equals(type)) {
                MaintenanceRequest out = workflowService.adminStartReview(user, request.getRequestId());
                return ResponseEntity.ok(out);
            }
            if ("PROJECT".equals(type)) {
                Project out = projectService.adminStartReview(request.getRequestId(), user);
                return ResponseEntity.ok(out);
            }
            if ("BOOKING".equals(type)) {
                Booking out = spaceService.adminStartReview(request.getRequestId(), user);
                return ResponseEntity.ok(out);
            }
            throw new com.org.cmbms.common.exception.ApiException("Unsupported requestType for this endpoint");
        } catch (Exception ex) {
            throw new com.org.cmbms.common.exception.ApiException(ex.getMessage());
        }
    }

    private com.org.cmbms.maintenance.dto.AssignSupervisorRequest map(AdminAssignRequest req) {
        com.org.cmbms.maintenance.dto.AssignSupervisorRequest out = new com.org.cmbms.maintenance.dto.AssignSupervisorRequest();
        out.setRequestId(req.getRequestId());
        out.setDivisionId(req.getDivisionId());
        out.setSupervisorId(req.getSupervisorId());
        out.setPriority(req.getPriority());
        return out;
    }
}

