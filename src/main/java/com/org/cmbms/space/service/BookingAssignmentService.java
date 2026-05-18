package com.org.cmbms.space.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ResourceNotFoundException;
import com.org.cmbms.space.dto.*;
import com.org.cmbms.space.model.BookingAssignment;
import com.org.cmbms.space.model.BookingReport;
import com.org.cmbms.space.repository.BookingAssignmentRepository;
import com.org.cmbms.space.repository.BookingReportRepository;
import com.org.cmbms.user.repository.UserRepository;
import com.org.cmbms.workflow.service.RequestLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingAssignmentService {
    
    private final BookingAssignmentRepository bookingAssignmentRepository;
    private final BookingReportRepository bookingReportRepository;
    private final SpaceService spaceService; // Use to get booking details
    private final RequestLifecycleService requestLifecycleService;
    private final UserRepository userRepository;
    
    /**
     * Normalize professional ID from frontend format (email or USR-XXX) to actual numeric ID
     */
    private String normalizeProfessionalId(String frontendId) {
        if (frontendId == null || frontendId.trim().isEmpty()) {
            return frontendId;
        }
        
        String cleanId = frontendId.trim();
        System.out.println("Normalizing professional ID: '" + cleanId + "'");
        
        // If it's a pure numeric ID, return as-is
        if (cleanId.matches("\\d+")) {
            return cleanId;
        }
        
        // If it's in USR-XXX format, extract the numeric ID
        if (cleanId.toUpperCase().startsWith("USR-")) {
            try {
                Long numericId = Long.parseLong(cleanId.substring(4));
                System.out.println("✅ Extracted numeric ID from USR format: " + numericId);
                return numericId.toString();
            } catch (NumberFormatException e) {
                // Fall through to email lookup
            }
        }
        
        // If it's an email, try to find the user by email (case-insensitive)
        if (cleanId.contains("@")) {
            String email = cleanId.toLowerCase();
            try {
                com.org.cmbms.user.model.User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    System.out.println("✅ Found user by email '" + email + "', numeric ID: " + user.getId());
                    return user.getId().toString();
                }
            } catch (Exception e) {
                System.err.println("Failed to find user by email: " + e.getMessage());
            }
            
            System.out.println("⚠️ Email not in local DB, using lowercased email: " + email);
            return email;
        }
        
        System.out.println("⚠️ Could not normalize ID, using as-is (lowercased): " + cleanId.toLowerCase());
        return cleanId.toLowerCase();
    }
    
    /**
     * Assign a professional to a booking with specific instructions/scope
     * Multiple professionals can be assigned to the same booking
     */
    @Transactional
    public BookingAssignmentDTO assignProfessionalToBooking(AssignProfessionalToBookingRequest request, UserPrincipal admin) {
        System.out.println("=== ASSIGNING PROFESSIONAL TO BOOKING ===");
        System.out.println("Booking ID: " + request.getBookingId());
        System.out.println("Professional ID (from frontend): " + request.getProfessionalId());
        System.out.println("Admin: " + admin.getId());
        
        // Normalize the professional ID
        String normalizedProfessionalId = normalizeProfessionalId(request.getProfessionalId());
        System.out.println("Professional ID (normalized): " + normalizedProfessionalId);
        
        // Verify booking exists
        com.org.cmbms.space.model.Booking booking = spaceService.getBookingById(request.getBookingId());
        if (booking == null) {
            System.err.println("❌ Booking not found: " + request.getBookingId());
            throw new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId());
        }
        System.out.println("✅ Booking exists");
        
        // Check if this professional is already assigned to this booking
        boolean alreadyAssigned = bookingAssignmentRepository.existsByBookingIdAndProfessionalId(
            request.getBookingId(), normalizedProfessionalId);
        System.out.println("Already assigned check: " + alreadyAssigned);
        
        if (alreadyAssigned) {
            System.err.println("❌ Professional already assigned to this booking");
            throw new IllegalArgumentException("Professional is already assigned to this booking");
        }
        
        // Get current assignments for this booking
        List<BookingAssignment> existingAssignments = bookingAssignmentRepository.findActiveAssignmentsByBookingId(request.getBookingId());
        System.out.println("Current active assignments for this booking: " + existingAssignments.size());
        for (BookingAssignment existing : existingAssignments) {
            System.out.println("  - Professional: " + existing.getProfessionalId());
        }
        
        // Create new assignment with normalized ID
        BookingAssignment assignment = new BookingAssignment();
        assignment.setBookingId(request.getBookingId());
        assignment.setProfessionalId(normalizedProfessionalId);
        assignment.setInstructions(request.getInstructions());
        assignment.setCreatedBy(admin.getId());
        assignment.setStatus("ACTIVE");
        
        System.out.println("💾 Saving assignment to database with normalized ID: " + normalizedProfessionalId);
        BookingAssignment saved = bookingAssignmentRepository.save(assignment);
        System.out.println("✅ Assignment saved with ID: " + saved.getId());
        
        // Update Booking status to ASSIGNED_TO_PROFESSIONALS if needed
        Status current = booking.getStatus();
        
        if (current == Status.SUBMITTED) {
            requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.UNDER_REVIEW, admin.getId());
            booking.setStatus(Status.UNDER_REVIEW);
            current = Status.UNDER_REVIEW;
        }
        
        if (current == Status.UNDER_REVIEW) {
            requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.ASSIGNED_TO_PROFESSIONALS, admin.getId());
            booking.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
            spaceService.updateBookingStatus(booking);
        } else if (current != Status.ASSIGNED_TO_PROFESSIONALS && current != Status.IN_PROGRESS) {
            System.err.println("⚠️ Warning: Booking is in " + current + " status. Assigned professional but did not transition status.");
        }
        
        // Send notification to professional
        sendNotificationToProfessional(saved);
        
        System.out.println("=== ASSIGNMENT COMPLETE ===");
        return convertToDTO(saved);
    }
    
    public List<BookingAssignmentDTO> getBookingAssignments(Long bookingId) {
        List<BookingAssignment> assignments = bookingAssignmentRepository.findByBookingId(bookingId);
        return assignments.stream()
                .filter(a -> !"INACTIVE".equals(a.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active assignments for a professional
     */
    public List<BookingAssignmentDTO> getProfessionalAssignments(String professionalId) {
        System.out.println("=== FETCHING ASSIGNMENTS FOR PROFESSIONAL ===");
        System.out.println("Source ID: " + professionalId);
        
        String cleanId = professionalId != null ? professionalId.trim() : "";
        String normalizedId = normalizeProfessionalId(cleanId);
        String lowerEmail = cleanId.contains("@") ? cleanId.toLowerCase() : null;
        String formattedUsrId = cleanId.matches("\\d+") ? String.format("USR-%03d", Integer.parseInt(cleanId)) : null;
        
        System.out.println("Searching with identifiers: [" + normalizedId + ", " + lowerEmail + ", " + formattedUsrId + "]");
        
        List<BookingAssignment> assignments = bookingAssignmentRepository.findByProfessionalId(normalizedId);
        
        // If searching by email/numeric ID didn't find everything, try the other formats
        if (lowerEmail != null && !lowerEmail.equals(normalizedId)) {
            List<BookingAssignment> emailAssignments = bookingAssignmentRepository.findByProfessionalId(lowerEmail);
            for (BookingAssignment ea : emailAssignments) {
                if (assignments.stream().noneMatch(a -> a.getId().equals(ea.getId()))) {
                    assignments.add(ea);
                }
            }
        }
        
        if (formattedUsrId != null && !formattedUsrId.equals(normalizedId)) {
            List<BookingAssignment> usrAssignments = bookingAssignmentRepository.findByProfessionalId(formattedUsrId);
            for (BookingAssignment ua : usrAssignments) {
                if (assignments.stream().noneMatch(a -> a.getId().equals(ua.getId()))) {
                    assignments.add(ua);
                }
            }
        }
        
        System.out.println("Found " + assignments.size() + " total matching assignments");
        
        return assignments.stream()
                .filter(a -> !"INACTIVE".equals(a.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get assignment details
     */
    public BookingAssignmentDTO getAssignmentDetails(Long assignmentId) {
        BookingAssignment assignment = bookingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        return convertToDTO(assignment);
    }
    
    /**
     * Submit a daily/periodic report for an assignment
     */
    @Transactional
    public BookingReportDTO submitReport(SubmitBookingReportRequest request, UserPrincipal professional) {
        // Verify assignment exists
        BookingAssignment assignment = bookingAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + request.getAssignmentId()));
        
        // Verify the professional is authorized to submit report for this assignment
        if (!assignment.getProfessionalId().equals(professional.getId())) {
            throw new IllegalArgumentException("Professional is not authorized to submit report for this assignment");
        }
        
        // Create report
        BookingReport report = new BookingReport();
        report.setAssignmentId(request.getAssignmentId());
        report.setReportText(request.getReportText());
        report.setCreatedBy(professional.getId());
        
        BookingReport saved = bookingReportRepository.save(report);
        
        // Notify admin about new report
        notifyAdminAboutNewReport(saved);
        
        return convertToDTO(saved);
    }
    
    /**
     * Get all reports for a specific assignment
     */
    public List<BookingReportDTO> getAssignmentReports(Long assignmentId) {
        List<BookingReport> reports = bookingReportRepository.findByAssignmentId(assignmentId);
        return reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all reports for a specific booking
     */
    public List<BookingReportDTO> getBookingReports(Long bookingId) {
        List<BookingReport> reports = bookingReportRepository.findByBookingId(bookingId);
        return reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all reports from a specific professional
     */
    public List<BookingReportDTO> getProfessionalReports(String professionalId) {
        List<BookingReport> reports = bookingReportRepository.findByProfessionalId(professionalId);
        return reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Deactivate an assignment (soft delete)
     */
    @Transactional
    public void deactivateAssignment(Long assignmentId) {
        BookingAssignment assignment = bookingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        assignment.setStatus("INACTIVE");
        bookingAssignmentRepository.save(assignment);
    }
    
    /**
     * Mark an assignment as started by the professional
     */
    @Transactional
    public void startAssignment(Long assignmentId, String professionalId) {
        BookingAssignment assignment = bookingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        
        // Normalize professional ID for comparison
        String normalizedProfId = normalizeProfessionalId(professionalId);
        if (!assignment.getProfessionalId().equals(normalizedProfId)) {
            System.err.println("❌ Auth failed: Assignment Prof ID: " + assignment.getProfessionalId() + 
                             ", User Prof ID: " + normalizedProfId);
            throw new IllegalArgumentException("Professional is not authorized to start this assignment");
        }
        
        assignment.setStatus("IN_PROGRESS");
        bookingAssignmentRepository.save(assignment);
        System.out.println("✅ Booking Assignment " + assignmentId + " marked as IN_PROGRESS by " + normalizedProfId);
    }

    /**
     * Mark an assignment as completed by the professional
     */
    @Transactional
    public void completeAssignment(Long assignmentId, String professionalId) {
        BookingAssignment assignment = bookingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        
        // Normalize professional ID for comparison
        String normalizedProfId = normalizeProfessionalId(professionalId);
        if (!assignment.getProfessionalId().equals(normalizedProfId)) {
            System.err.println("❌ Auth failed: Assignment Prof ID: " + assignment.getProfessionalId() + 
                             ", User Prof ID: " + normalizedProfId);
            throw new IllegalArgumentException("Professional is not authorized to complete this assignment");
        }
        
        assignment.setStatus("COMPLETED");
        bookingAssignmentRepository.save(assignment);
        System.out.println("✅ Booking Assignment " + assignmentId + " marked as COMPLETED by " + normalizedProfId);
    }
    
    /**
     * Mark all reports for an assignment as read
     */
    @Transactional
    public void markAssignmentReportsAsRead(Long assignmentId) {
        List<BookingReport> reports = bookingReportRepository.findByAssignmentId(assignmentId);
        reports.forEach(r -> r.setViewed(true));
        bookingReportRepository.saveAll(reports);
    }
    
    // ===== HELPER METHODS =====
    
    private void sendNotificationToProfessional(BookingAssignment assignment) {
        try {
            // Fetch booking details for notification
            com.org.cmbms.space.model.Booking booking = spaceService.getBookingById(assignment.getBookingId());
            String notificationTitle = "New Booking Assignment";
            String notificationMessage = "Booking " + booking.getBookingId() + " has been assigned to you";
            requestLifecycleService.notifyUser(assignment.getProfessionalId(), notificationTitle, notificationMessage);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }
    
    private void notifyAdminAboutNewReport(BookingReport report) {
        try {
            BookingAssignment assignment = bookingAssignmentRepository.findById(report.getAssignmentId()).orElse(null);
            if (assignment != null) {
                com.org.cmbms.space.model.Booking booking = spaceService.getBookingById(assignment.getBookingId());
                String notificationTitle = "New Booking Report Submitted";
                String notificationMessage = "A new report has been submitted for booking " + booking.getBookingId();
                requestLifecycleService.notifyUsersByRole(com.org.cmbms.common.enums.Role.ADMIN, notificationTitle, notificationMessage);
            }
        } catch (Exception e) {
            System.err.println("Failed to notify admin: " + e.getMessage());
        }
    }
    
    private BookingAssignmentDTO convertToDTO(BookingAssignment assignment) {
        return new BookingAssignmentDTO(
                assignment.getId(),
                assignment.getBookingId(),
                assignment.getProfessionalId(),
                assignment.getInstructions(),
                assignment.getCreatedAt(),
                assignment.getCreatedBy(),
                assignment.getStatus()
        );
    }
    
    private BookingReportDTO convertToDTO(BookingReport report) {
        return new BookingReportDTO(
                report.getId(),
                report.getAssignmentId(),
                report.getReportText(),
                report.getCreatedAt(),
                report.getCreatedBy(),
                report.isViewed()
        );
    }
}
