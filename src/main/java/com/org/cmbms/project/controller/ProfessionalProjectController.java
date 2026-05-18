package com.org.cmbms.project.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.project.dto.ProjectAssignmentDTO;
import com.org.cmbms.project.dto.ProfessionalReportDTO;
import com.org.cmbms.project.dto.SubmitProfessionalReportRequest;
import com.org.cmbms.project.service.ProjectAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professional/projects")
@RequiredArgsConstructor
@Validated
public class ProfessionalProjectController {
    
    private final ProjectAssignmentService projectAssignmentService;
    
    /**
     * Get all assigned projects for the logged-in professional
     * GET /api/professional/projects/my-assignments
     */
    @GetMapping("/my-assignments")
    public ResponseEntity<List<ProjectAssignmentDTO>> getMyAssignments() {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        System.out.println("🔍 Professional requesting assignments:");
        System.out.println("   ID: " + professional.getId());
        System.out.println("   Email: " + professional.getEmail());
        System.out.println("   Role: " + professional.getRole());
        
        List<ProjectAssignmentDTO> assignments = projectAssignmentService.getProfessionalAssignments(professional.getId());
        
        System.out.println("📦 Returning " + assignments.size() + " assignments to frontend");
        return ResponseEntity.ok(assignments);
    }
    
    /**
     * Get assignment details
     * GET /api/professional/projects/assignments/{assignmentId}
     */
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<ProjectAssignmentDTO> getAssignmentDetails(@PathVariable Long assignmentId) {
        ProjectAssignmentDTO assignment = projectAssignmentService.getAssignmentDetails(assignmentId);
        return ResponseEntity.ok(assignment);
    }
    
    /**
     * Submit a daily/periodic report for an assignment
     * POST /api/professional/projects/assignments/{assignmentId}/report
     */
    @PostMapping("/assignments/{assignmentId}/report")
    public ResponseEntity<ProfessionalReportDTO> submitReport(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmitProfessionalReportRequest request) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        
        // Update assignmentId in request if not set
        request.setAssignmentId(assignmentId);
        
        ProfessionalReportDTO report = projectAssignmentService.submitReport(request, professional);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }
    
    /**
     * Get all reports submitted for an assignment (professional can only see their own)
     * GET /api/professional/projects/assignments/{assignmentId}/my-reports
     */
    @GetMapping("/assignments/{assignmentId}/my-reports")
    public ResponseEntity<List<ProfessionalReportDTO>> getMyAssignmentReports(@PathVariable Long assignmentId) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        
        // Verify professional is authorized to see these reports
        ProjectAssignmentDTO assignment = projectAssignmentService.getAssignmentDetails(assignmentId);
        if (!assignment.getProfessionalId().equals(professional.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ProfessionalReportDTO> reports = projectAssignmentService.getAssignmentReports(assignmentId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Mark an assignment as completed
     * PATCH /api/professional/projects/assignments/{assignmentId}/complete
     */
    @PatchMapping("/assignments/{assignmentId}/complete")
    public ResponseEntity<Void> completeAssignment(@PathVariable Long assignmentId) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        projectAssignmentService.completeAssignment(assignmentId, professional.getId());
        return ResponseEntity.ok().build();
    }
    /**
     * Mark an assignment as started
     * PATCH /api/professional/projects/assignments/{assignmentId}/start
     */
    @PatchMapping("/assignments/{assignmentId}/start")
    public ResponseEntity<Void> startAssignment(@PathVariable Long assignmentId) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        projectAssignmentService.startAssignment(assignmentId, professional.getId());
        return ResponseEntity.ok().build();
    }
}
