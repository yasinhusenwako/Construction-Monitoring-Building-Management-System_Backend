package com.org.cmbms.space.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.space.dto.*;
import com.org.cmbms.space.service.BookingAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professional/bookings")
@RequiredArgsConstructor
public class ProfessionalBookingAssignmentController {
    
    private final BookingAssignmentService bookingAssignmentService;
    
    /**
     * Get all my assignments
     * GET /api/professional/bookings/my-assignments
     */
    @GetMapping("/my-assignments")
    public ResponseEntity<List<BookingAssignmentDTO>> getMyAssignments() {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        System.out.println("🔍 Professional requesting booking assignments:");
        System.out.println("   ID: " + professional.getId());
        System.out.println("   Email: " + professional.getEmail());
        System.out.println("   Role: " + professional.getRole());
        
        List<BookingAssignmentDTO> assignments = bookingAssignmentService.getProfessionalAssignments(professional.getId());
        
        System.out.println("📦 Returning " + assignments.size() + " booking assignments to frontend");
        return ResponseEntity.ok(assignments);
    }
    
    /**
     * Get assignment details
     * GET /api/professional/bookings/assignments/{assignmentId}
     */
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<BookingAssignmentDTO> getAssignmentDetails(@PathVariable Long assignmentId) {
        BookingAssignmentDTO assignment = bookingAssignmentService.getAssignmentDetails(assignmentId);
        return ResponseEntity.ok(assignment);
    }
    
    /**
     * Submit a report for an assignment
     * POST /api/professional/bookings/assignments/{assignmentId}/report
     */
    @PostMapping("/assignments/{assignmentId}/report")
    public ResponseEntity<BookingReportDTO> submitReport(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmitBookingReportRequest request) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        
        request.setAssignmentId(assignmentId);
        BookingReportDTO report = bookingAssignmentService.submitReport(request, professional);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }
    
    /**
     * Get my reports for an assignment
     * GET /api/professional/bookings/assignments/{assignmentId}/my-reports
     */
    @GetMapping("/assignments/{assignmentId}/my-reports")
    public ResponseEntity<List<BookingReportDTO>> getMyReports(@PathVariable Long assignmentId) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        
        // Verify professional is authorized to see these reports
        BookingAssignmentDTO assignment = bookingAssignmentService.getAssignmentDetails(assignmentId);
        if (!assignment.getProfessionalId().equals(professional.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<BookingReportDTO> reports = bookingAssignmentService.getAssignmentReports(assignmentId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Mark assignment complete
     * PATCH /api/professional/bookings/assignments/{assignmentId}/complete
     */
    @PatchMapping("/assignments/{assignmentId}/complete")
    public ResponseEntity<Void> completeAssignment(@PathVariable Long assignmentId) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        bookingAssignmentService.completeAssignment(assignmentId, professional.getId());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Start assignment (mark as in progress)
     * PATCH /api/professional/bookings/assignments/{assignmentId}/start
     */
    @PatchMapping("/assignments/{assignmentId}/start")
    public ResponseEntity<Void> startAssignment(@PathVariable Long assignmentId) {
        UserPrincipal professional = SecurityUtils.getCurrentUser();
        bookingAssignmentService.startAssignment(assignmentId, professional.getId());
        return ResponseEntity.ok().build();
    }
}
