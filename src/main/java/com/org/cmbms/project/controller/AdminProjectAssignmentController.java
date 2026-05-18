package com.org.cmbms.project.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.project.dto.*;
import com.org.cmbms.project.service.ProjectAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
@Validated
public class AdminProjectAssignmentController {
    
    private final ProjectAssignmentService projectAssignmentService;
    
    /**
     * Assign a professional to a project with specific instructions/scope
     * POST /api/admin/projects/{projectId}/assign-professional
     */
    @PostMapping("/{projectId}/assign-professional")
    public ResponseEntity<?> assignProfessionalToProject(
            @PathVariable Long projectId,
            @Valid @RequestBody AssignProfessionalToProjectRequest request) {
        try {
            UserPrincipal admin = SecurityUtils.getCurrentUser();
            
            // Check if user is admin
            if (admin.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", "Only admins can assign professionals to projects")
                );
            }
            
            System.out.println("DEBUG: Assign Professional Request - ProjectId: " + projectId + ", ProfessionalId: " + request.getProfessionalId());
            
            // Update projectId in request if not set
            request.setProjectId(projectId);
            
            ProjectAssignmentDTO assignment = projectAssignmentService.assignProfessionalToProject(request, admin);
            return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
        } catch (IllegalArgumentException e) {
            System.err.println("VALIDATION ERROR: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis())
            );
        } catch (Exception e) {
            System.err.println("ASSIGNMENT ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "error", e.getMessage() != null ? e.getMessage() : "Internal server error",
                            "type", e.getClass().getSimpleName(),
                            "timestamp", System.currentTimeMillis()
                    )
            );
        }
    }
    
    /**
     * Get all assignments for a project
     * GET /api/admin/projects/{projectId}/assignments
     */
    @GetMapping("/{projectId}/assignments")
    public ResponseEntity<List<ProjectAssignmentDTO>> getProjectAssignments(@PathVariable Long projectId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ProjectAssignmentDTO> assignments = projectAssignmentService.getProjectAssignments(projectId);
        return ResponseEntity.ok(assignments);
    }
    
    /**
     * Get all reports for a project (admin only)
     * GET /api/admin/projects/{projectId}/reports
     */
    @GetMapping("/{projectId}/reports")
    public ResponseEntity<List<ProfessionalReportDTO>> getProjectReports(@PathVariable Long projectId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ProfessionalReportDTO> reports = projectAssignmentService.getProjectReports(projectId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get all reports from a specific professional (admin only)
     * GET /api/admin/projects/professional/{professionalId}/reports
     */
    @GetMapping("/professional/{professionalId}/reports")
    public ResponseEntity<List<ProfessionalReportDTO>> getProfessionalReports(@PathVariable String professionalId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ProfessionalReportDTO> reports = projectAssignmentService.getProfessionalReports(professionalId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get reports for a specific assignment
     * GET /api/admin/projects/assignments/{assignmentId}/reports
     */
    @GetMapping("/assignments/{assignmentId}/reports")
    public ResponseEntity<List<ProfessionalReportDTO>> getAssignmentReports(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ProfessionalReportDTO> reports = projectAssignmentService.getAssignmentReports(assignmentId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Deactivate an assignment
     * DELETE /api/admin/projects/assignments/{assignmentId}
     */
    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> deactivateAssignment(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        projectAssignmentService.deactivateAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Assignment deactivated successfully"));
    }
    
    /**
     * Mark all reports for an assignment as read
     * PATCH /api/admin/projects/assignments/{assignmentId}/read
     */
    @PatchMapping("/assignments/{assignmentId}/read")
    public ResponseEntity<?> markReportsAsRead(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        projectAssignmentService.markAssignmentReportsAsRead(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Reports marked as read"));
    }

    /**
     * Mark an assignment as needing clarification
     * PATCH /api/admin/projects/assignments/{assignmentId}/clarify
     */
    @PatchMapping("/assignments/{assignmentId}/clarify")
    public ResponseEntity<?> requestClarification(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        projectAssignmentService.requestClarification(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Clarification requested"));
    }

    /**
     * Mark an assignment as approved
     * PATCH /api/admin/projects/assignments/{assignmentId}/approve
     */
    @PatchMapping("/assignments/{assignmentId}/approve")
    public ResponseEntity<?> approveAssignment(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        projectAssignmentService.approveAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Assignment approved"));
    }

    /**
     * Mark an assignment as rejected
     * PATCH /api/admin/projects/assignments/{assignmentId}/reject
     */
    @PatchMapping("/assignments/{assignmentId}/reject")
    public ResponseEntity<?> rejectAssignment(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        projectAssignmentService.rejectAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Assignment rejected"));
    }
}
