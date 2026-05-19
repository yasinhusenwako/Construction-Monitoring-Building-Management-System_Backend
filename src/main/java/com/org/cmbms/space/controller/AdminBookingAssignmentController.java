package com.org.cmbms.space.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.space.dto.*;
import com.org.cmbms.space.service.BookingAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@Validated
public class AdminBookingAssignmentController {
    
    private final BookingAssignmentService bookingAssignmentService;
    
    /**
     * Assign a professional to a booking with specific instructions/scope
     * POST /api/admin/bookings/{bookingId}/assign-professional
     */
    @PostMapping("/{bookingId}/assign-professional")
    public ResponseEntity<?> assignProfessionalToBooking(
            @PathVariable Long bookingId,
            @RequestBody AssignProfessionalToBookingRequest request) {
        try {
            UserPrincipal admin = SecurityUtils.getCurrentUser();
            
            // Check if user is admin
            if (admin.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", "Only admins can assign professionals to bookings")
                );
            }
            
            System.out.println("DEBUG: Assign Professional Request - BookingId: " + bookingId + ", ProfessionalId: " + request.getProfessionalId());
            
            // Validate request
            if (request.getProfessionalId() == null || request.getProfessionalId().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", "Professional ID is required")
                );
            }
            if (request.getInstructions() == null || request.getInstructions().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", "Instructions are required")
                );
            }
            
            // Update bookingId in request
            request.setBookingId(bookingId);
            
            BookingAssignmentDTO assignment = bookingAssignmentService.assignProfessionalToBooking(request, admin);
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
     * Get all assignments for a booking
     * GET /api/admin/bookings/{bookingId}/assignments
     */
    @GetMapping("/{bookingId}/assignments")
    public ResponseEntity<List<BookingAssignmentDTO>> getBookingAssignments(@PathVariable Long bookingId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<BookingAssignmentDTO> assignments = bookingAssignmentService.getBookingAssignments(bookingId);
        return ResponseEntity.ok(assignments);
    }
    
    /**
     * Get all reports for a booking (admin only)
     * GET /api/admin/bookings/{bookingId}/reports
     */
    @GetMapping("/{bookingId}/reports")
    public ResponseEntity<List<BookingReportDTO>> getBookingReports(@PathVariable Long bookingId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<BookingReportDTO> reports = bookingAssignmentService.getBookingReports(bookingId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get all reports from a specific professional (admin only)
     * GET /api/admin/bookings/professional/{professionalId}/reports
     */
    @GetMapping("/professional/{professionalId}/reports")
    public ResponseEntity<List<BookingReportDTO>> getProfessionalReports(@PathVariable String professionalId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<BookingReportDTO> reports = bookingAssignmentService.getProfessionalReports(professionalId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get reports for a specific assignment
     * GET /api/admin/bookings/assignments/{assignmentId}/reports
     */
    @GetMapping("/assignments/{assignmentId}/reports")
    public ResponseEntity<List<BookingReportDTO>> getAssignmentReports(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<BookingReportDTO> reports = bookingAssignmentService.getAssignmentReports(assignmentId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Deactivate an assignment
     * DELETE /api/admin/bookings/assignments/{assignmentId}
     */
    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> deactivateAssignment(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        bookingAssignmentService.deactivateAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Assignment deactivated successfully"));
    }
    
    /**
     * Mark all reports for an assignment as read
     * PATCH /api/admin/bookings/assignments/{assignmentId}/read
     */
    @PatchMapping("/assignments/{assignmentId}/read")
    public ResponseEntity<?> markReportsAsRead(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        bookingAssignmentService.markAssignmentReportsAsRead(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Reports marked as read"));
    }

    /**
     * Request clarification on a booking assignment
     * PATCH /api/admin/bookings/assignments/{assignmentId}/clarify
     */
    @PatchMapping("/assignments/{assignmentId}/clarify")
    public ResponseEntity<?> requestClarification(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        bookingAssignmentService.requestClarification(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Clarification requested"));
    }

    /**
     * Approve a booking assignment
     * PATCH /api/admin/bookings/assignments/{assignmentId}/approve
     */
    @PatchMapping("/assignments/{assignmentId}/approve")
    public ResponseEntity<?> approveAssignment(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        bookingAssignmentService.approveAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Assignment approved"));
    }

    /**
     * Reject a booking assignment
     * PATCH /api/admin/bookings/assignments/{assignmentId}/reject
     */
    @PatchMapping("/assignments/{assignmentId}/reject")
    public ResponseEntity<?> rejectAssignment(@PathVariable Long assignmentId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getRole() != com.org.cmbms.common.enums.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        bookingAssignmentService.rejectAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Assignment rejected"));
    }
}
