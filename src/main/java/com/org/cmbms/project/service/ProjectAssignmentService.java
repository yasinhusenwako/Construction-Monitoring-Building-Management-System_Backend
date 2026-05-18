package com.org.cmbms.project.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ResourceNotFoundException;
import com.org.cmbms.project.dto.*;
import com.org.cmbms.project.model.ProjectAssignment;
import com.org.cmbms.project.model.ProfessionalReport;
import com.org.cmbms.project.model.Project;
import com.org.cmbms.project.repository.ProjectAssignmentRepository;
import com.org.cmbms.project.repository.ProfessionalReportRepository;
import com.org.cmbms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectAssignmentService {
    
    private final ProjectAssignmentRepository projectAssignmentRepository;
    private final ProfessionalReportRepository professionalReportRepository;
    private final ProjectRepository projectRepository;
    private final com.org.cmbms.workflow.service.RequestLifecycleService requestLifecycleService;
    private final com.org.cmbms.user.repository.UserRepository userRepository;
    
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
                // First try direct lookup
                com.org.cmbms.user.model.User user = userRepository.findByEmail(email).orElse(null);
                
                // If not found, try a case-insensitive search if needed (though findByEmail should handle it)
                if (user != null) {
                    System.out.println("✅ Found user by email '" + email + "', numeric ID: " + user.getId());
                    return user.getId().toString();
                }
            } catch (Exception e) {
                System.err.println("Failed to find user by email: " + e.getMessage());
            }
            
            // If still not found in local DB, return the lowercased email
            // This ensures consistency even if the user hasn't been synced to local DB yet
            System.out.println("⚠️ Email not in local DB, using lowercased email: " + email);
            return email;
        }
        
        // If lookup failed, return lowercased as-is
        System.out.println("⚠️ Could not normalize ID, using as-is (lowercased): " + cleanId.toLowerCase());
        return cleanId.toLowerCase();
    }
    
    /**
     * Assign a professional to a project with specific instructions/scope
     * Multiple professionals can be assigned to the same project
     */
    @Transactional
    public ProjectAssignmentDTO assignProfessionalToProject(AssignProfessionalToProjectRequest request, UserPrincipal admin) {
        System.out.println("=== ASSIGNING PROFESSIONAL TO PROJECT ===");
        System.out.println("Project ID: " + request.getProjectId());
        System.out.println("Professional ID (from frontend): " + request.getProfessionalId());
        System.out.println("Admin: " + admin.getId());
        
        // Normalize the professional ID
        String normalizedProfessionalId = normalizeProfessionalId(request.getProfessionalId());
        System.out.println("Professional ID (normalized): " + normalizedProfessionalId);
        
        // Verify project exists
        if (!projectRepository.existsById(request.getProjectId())) {
            System.err.println("❌ Project not found: " + request.getProjectId());
            throw new ResourceNotFoundException("Project not found with ID: " + request.getProjectId());
        }
        System.out.println("✅ Project exists");
        
        // Check if this professional is already assigned to this project
        boolean alreadyAssigned = projectAssignmentRepository.existsByProjectIdAndProfessionalId(
            request.getProjectId(), normalizedProfessionalId);
        System.out.println("Already assigned check: " + alreadyAssigned);
        
        if (alreadyAssigned) {
            System.err.println("❌ Professional already assigned to this project");
            throw new IllegalArgumentException("Professional is already assigned to this project");
        }
        
        // Get current assignments for this project
        List<ProjectAssignment> existingAssignments = projectAssignmentRepository.findActiveAssignmentsByProjectId(request.getProjectId());
        System.out.println("Current active assignments for this project: " + existingAssignments.size());
        for (ProjectAssignment existing : existingAssignments) {
            System.out.println("  - Professional: " + existing.getProfessionalId());
        }
        
        // Create new assignment with normalized ID
        ProjectAssignment assignment = new ProjectAssignment();
        assignment.setProjectId(request.getProjectId());
        assignment.setProfessionalId(normalizedProfessionalId);
        assignment.setInstructions(request.getInstructions());
        assignment.setCreatedBy(admin.getId());
        assignment.setStatus("ACTIVE");
        
        System.out.println("💾 Saving assignment to database with normalized ID: " + normalizedProfessionalId);
        ProjectAssignment saved = projectAssignmentRepository.save(assignment);
        System.out.println("✅ Assignment saved with ID: " + saved.getId());
        
        // Update Project status to ASSIGNED_TO_PROFESSIONALS if needed
        Project project = projectRepository.findById(request.getProjectId()).orElseThrow();
        com.org.cmbms.common.enums.Status current = project.getStatus();
        
        if (current == com.org.cmbms.common.enums.Status.SUBMITTED) {
            requestLifecycleService.transition(com.org.cmbms.common.enums.RequestType.PROJECT, project.getId(), com.org.cmbms.common.enums.Status.UNDER_REVIEW, admin.getId());
            project.setStatus(com.org.cmbms.common.enums.Status.UNDER_REVIEW);
            current = com.org.cmbms.common.enums.Status.UNDER_REVIEW;
        }
        
        if (current == com.org.cmbms.common.enums.Status.UNDER_REVIEW) {
            requestLifecycleService.transition(com.org.cmbms.common.enums.RequestType.PROJECT, project.getId(), com.org.cmbms.common.enums.Status.ASSIGNED_TO_PROFESSIONALS, admin.getId());
            project.setStatus(com.org.cmbms.common.enums.Status.ASSIGNED_TO_PROFESSIONALS);
            projectRepository.save(project);
        } else if (current != com.org.cmbms.common.enums.Status.ASSIGNED_TO_PROFESSIONALS && current != com.org.cmbms.common.enums.Status.IN_PROGRESS) {
             System.err.println("⚠️ Warning: Project is in " + current + " status. Assigned professional but did not transition status.");
        }
        
        // Send notification to professional
        sendNotificationToProfessional(saved);
        
        System.out.println("=== ASSIGNMENT COMPLETE ===");
        return convertToDTO(saved);
    }
    
    public List<ProjectAssignmentDTO> getProjectAssignments(Long projectId) {
        List<ProjectAssignment> assignments = projectAssignmentRepository.findByProjectId(projectId);
        return assignments.stream()
                .filter(a -> !"INACTIVE".equals(a.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active assignments for a professional
     */
    public List<ProjectAssignmentDTO> getProfessionalAssignments(String professionalId) {
        System.out.println("=== FETCHING ASSIGNMENTS FOR PROFESSIONAL ===");
        System.out.println("Source ID: " + professionalId);
        
        String cleanId = professionalId != null ? professionalId.trim() : "";
        String normalizedId = normalizeProfessionalId(cleanId);
        String lowerEmail = cleanId.contains("@") ? cleanId.toLowerCase() : null;
        String formattedUsrId = cleanId.matches("\\d+") ? String.format("USR-%03d", Integer.parseInt(cleanId)) : null;
        
        System.out.println("Searching with identifiers: [" + normalizedId + ", " + lowerEmail + ", " + formattedUsrId + "]");
        
        // Fetch all assignments that match ANY of these identifiers
        List<ProjectAssignment> assignments = projectAssignmentRepository.findByProfessionalId(normalizedId);
        
        // If searching by email/numeric ID didn't find everything, try the other formats
        if (lowerEmail != null && !lowerEmail.equals(normalizedId)) {
            List<ProjectAssignment> emailAssignments = projectAssignmentRepository.findByProfessionalId(lowerEmail);
            for (ProjectAssignment ea : emailAssignments) {
                if (assignments.stream().noneMatch(a -> a.getId().equals(ea.getId()))) {
                    assignments.add(ea);
                }
            }
        }
        
        if (formattedUsrId != null && !formattedUsrId.equals(normalizedId)) {
            List<ProjectAssignment> usrAssignments = projectAssignmentRepository.findByProfessionalId(formattedUsrId);
            for (ProjectAssignment ua : usrAssignments) {
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
    public ProjectAssignmentDTO getAssignmentDetails(Long assignmentId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        return convertToDTO(assignment);
    }
    
    /**
     * Submit a daily/periodic report for an assignment
     */
    @Transactional
    public ProfessionalReportDTO submitReport(SubmitProfessionalReportRequest request, UserPrincipal professional) {
        // Verify assignment exists
        ProjectAssignment assignment = projectAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + request.getAssignmentId()));
        
        // Verify the professional is authorized to submit report for this assignment
        if (!assignment.getProfessionalId().equals(professional.getId())) {
            throw new IllegalArgumentException("Professional is not authorized to submit report for this assignment");
        }
        
        // Create report
        ProfessionalReport report = new ProfessionalReport();
        report.setAssignmentId(request.getAssignmentId());
        report.setReportText(request.getReportText());
        report.setCreatedBy(professional.getId());
        
        ProfessionalReport saved = professionalReportRepository.save(report);
        
        // TODO: Notify admin about new report
        notifyAdminAboutNewReport(saved);
        
        return convertToDTO(saved);
    }
    
    /**
     * Get all reports for a specific assignment (ordered by most recent first)
     */
    public List<ProfessionalReportDTO> getAssignmentReports(Long assignmentId) {
        List<ProfessionalReport> reports = professionalReportRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
        return reports.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get all reports for a specific project (admin only)
     */
    public List<ProfessionalReportDTO> getProjectReports(Long projectId) {
        List<ProfessionalReport> reports = professionalReportRepository.findReportsByProjectId(projectId);
        return reports.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get all reports submitted by a professional (admin only)
     */
    public List<ProfessionalReportDTO> getProfessionalReports(String professionalId) {
        List<ProfessionalReport> reports = professionalReportRepository.findReportsByProfessionalId(professionalId);
        return reports.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Deactivate an assignment
     */
    /**
     * Mark an assignment as started by the professional
     */
    @Transactional
    public void startAssignment(Long assignmentId, String professionalId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        
        // Normalize professional ID for comparison
        String normalizedProfId = normalizeProfessionalId(professionalId);
        if (!assignment.getProfessionalId().equals(normalizedProfId)) {
            System.err.println("❌ Auth failed: Assignment Prof ID: " + assignment.getProfessionalId() + 
                             ", User Prof ID: " + normalizedProfId);
            throw new IllegalArgumentException("Professional is not authorized to start this assignment");
        }
        
        assignment.setStatus("IN_PROGRESS");
        projectAssignmentRepository.save(assignment);
        System.out.println("✅ Assignment " + assignmentId + " marked as IN_PROGRESS by " + normalizedProfId);
        
        // Update Project status to IN_PROGRESS if it's currently ASSIGNED_TO_PROFESSIONALS
        Long projectId = assignment.getProjectId();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null && project.getStatus() == com.org.cmbms.common.enums.Status.ASSIGNED_TO_PROFESSIONALS) {
            System.out.println("🚀 First professional started task. Transitioning project " + projectId + " to IN_PROGRESS.");
            requestLifecycleService.transition(
                com.org.cmbms.common.enums.RequestType.PROJECT, 
                project.getId(), 
                com.org.cmbms.common.enums.Status.IN_PROGRESS, 
                0L // System/Auto trigger
            );
            project.setStatus(com.org.cmbms.common.enums.Status.IN_PROGRESS);
            projectRepository.save(project);
        }
    }

    /**
     * Mark an assignment as completed by the professional
     */
    @Transactional
    public void completeAssignment(Long assignmentId, String professionalId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        
        // Normalize professional ID for comparison
        String normalizedProfId = normalizeProfessionalId(professionalId);
        if (!assignment.getProfessionalId().equals(normalizedProfId)) {
            System.err.println("❌ Auth failed: Assignment Prof ID: " + assignment.getProfessionalId() + 
                             ", User Prof ID: " + normalizedProfId);
            throw new IllegalArgumentException("Professional is not authorized to complete this assignment");
        }
        
        assignment.setStatus("COMPLETED");
        projectAssignmentRepository.save(assignment);
        System.out.println("✅ Assignment " + assignmentId + " marked as COMPLETED by " + normalizedProfId);
    }

    /**
     * Mark an assignment as approved by the admin
     */
    @Transactional
    public void approveAssignment(Long assignmentId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        assignment.setStatus("APPROVED");
        projectAssignmentRepository.save(assignment);
        System.out.println("✅ Assignment " + assignmentId + " marked as APPROVED");
    }

    /**
     * Mark an assignment as rejected by the admin
     */
    @Transactional
    public void rejectAssignment(Long assignmentId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        assignment.setStatus("REJECTED");
        projectAssignmentRepository.save(assignment);
        System.out.println("✅ Assignment " + assignmentId + " marked as REJECTED");
    }

    @Transactional
    public void deactivateAssignment(Long assignmentId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));
        assignment.setStatus("INACTIVE");
        projectAssignmentRepository.save(assignment);
    }
    
    /**
     * Mark all reports for an assignment as viewed
     */
    @Transactional
    public void markAssignmentReportsAsRead(Long assignmentId) {
        List<ProfessionalReport> reports = professionalReportRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
        boolean changed = false;
        for (ProfessionalReport report : reports) {
            if (!report.isViewed()) {
                report.setViewed(true);
                changed = true;
            }
        }
        if (changed) {
            professionalReportRepository.saveAll(reports);
            System.out.println("✅ Marked reports as read for assignment: " + assignmentId);
        }
    }
    
    // Helper methods
    private ProjectAssignmentDTO convertToDTO(ProjectAssignment assignment) {
        return new ProjectAssignmentDTO(
                assignment.getId(),
                assignment.getProjectId(),
                assignment.getProfessionalId(),
                assignment.getInstructions(),
                assignment.getCreatedAt(),
                assignment.getCreatedBy(),
                assignment.getStatus()
        );
    }
    
    private ProfessionalReportDTO convertToDTO(ProfessionalReport report) {
        return new ProfessionalReportDTO(
                report.getId(),
                report.getAssignmentId(),
                report.getReportText(),
                report.getCreatedAt(),
                report.getCreatedBy(),
                report.isViewed()
        );
    }
    
    private void sendNotificationToProfessional(ProjectAssignment assignment) {
        // Send notification to professional using the existing notification system
        try {
            String projectInfo = "Project ID: " + assignment.getProjectId();
            String message = "You have been assigned to " + projectInfo + ". Instructions: " + 
                           (assignment.getInstructions().length() > 100 
                               ? assignment.getInstructions().substring(0, 100) + "..." 
                               : assignment.getInstructions());
            
            requestLifecycleService.notifyUser(
                assignment.getProfessionalId(), 
                "New Project Assignment", 
                message
            );
            
            System.out.println("✅ Notification sent to professional: " + assignment.getProfessionalId() + 
                    " for project assignment ID: " + assignment.getId());
        } catch (Exception e) {
            System.err.println("❌ Failed to send notification to professional: " + assignment.getProfessionalId() + 
                    " - Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void notifyAdminAboutNewReport(ProfessionalReport report) {
        // Notify all admins about new report submission
        try {
            ProjectAssignment assignment = projectAssignmentRepository.findById(report.getAssignmentId())
                    .orElse(null);
            
            String message = "New report submitted for assignment ID: " + report.getAssignmentId();
            if (assignment != null) {
                message += " (Project ID: " + assignment.getProjectId() + 
                          ", Professional: " + assignment.getProfessionalId() + ")";
            }
            
            requestLifecycleService.notifyUsersByRole(
                com.org.cmbms.common.enums.Role.ADMIN, 
                "New Professional Report", 
                message
            );
            
            System.out.println("✅ Admin notification sent for new report ID: " + report.getId());
        } catch (Exception e) {
            System.err.println("❌ Failed to notify admins about new report: " + report.getId() + 
                    " - Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mark an assignment as needing clarification
     */
    @Transactional
    public void requestClarification(Long assignmentId) {
        ProjectAssignment assignment = projectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        
        assignment.setStatus("NEEDS_CLARIFICATION");
        projectAssignmentRepository.save(assignment);
        
        // Also ensure project status is back to "Assigned to Professionals" 
        // in case it was prematurely moved to something else
        Project project = projectRepository.findById(assignment.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (Status.COMPLETED.equals(project.getStatus())) {
            project.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
            projectRepository.save(project);
        }
    }
}
