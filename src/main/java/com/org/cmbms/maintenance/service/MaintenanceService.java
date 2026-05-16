
package com.org.cmbms.maintenance.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
import com.org.cmbms.maintenance.dto.CreateMaintenanceRequestDTO;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.repository.MaintenanceRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import com.org.cmbms.workflow.service.RequestLifecycleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final WorkflowService workflowService;
    private final com.org.cmbms.file.service.FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RequestLifecycleService requestLifecycleService;

    public MaintenanceRequest create(CreateMaintenanceRequestDTO dto, UserPrincipal user) {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setMaintenanceId(dto.getMaintenanceId());
        request.setCategory(dto.getCategory());
        request.setPriority(dto.getPriority());
        request.setDescription(dto.getDescription());
        request.setLocation(dto.getLocation());
        request.setCreatedBy(user.getId());
        request.setCreatedAt(LocalDateTime.now());
        if (dto.getDivisionId() != null) {
            DivisionRules.assertAllowed(dto.getDivisionId());
        }
        request.setDivisionId(dto.getDivisionId());
        MaintenanceRequest saved = maintenanceRepository.save(request);
        Long userNumericId = user.getNumericId() != null ? user.getNumericId() : 0L;
        workflowService.initializeSubmittedStatus(saved, userNumericId);
        
        // Notify all admins (both database and Keycloak) about new maintenance submission
        requestLifecycleService.notifyUsersByRole(Role.ADMIN, "New Maintenance Request", 
            "Maintenance " + saved.getMaintenanceId() + " has been submitted by " + user.getEmail());
        
        return maintenanceRepository.save(saved);
    }

    public com.org.cmbms.file.model.FileRecord uploadDoc(Long id, org.springframework.web.multipart.MultipartFile file, Long userId) throws java.io.IOException {
        return fileStorageService.save(id, "MAINTENANCE_DOC", file, userId);
    }

    public MaintenanceRequest update(Long id, CreateMaintenanceRequestDTO dto, UserPrincipal user) {
        MaintenanceRequest request = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Maintenance request not found"));
        
        if (user.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot update maintenance requests");
        }
        
        if (request.getStatus() != Status.SUBMITTED && request.getStatus() != Status.UNDER_REVIEW && user.getRole() != Role.ADMIN) {
            throw new ApiException("Maintenance request cannot be edited in its current status");
        }
        
        // Ownership check
        if (user.getRole() != Role.ADMIN && !request.getCreatedBy().equals(user.getId())) {
            throw new ApiException("You are not authorized to edit this request");
        }

        if (dto.getCategory() != null) request.setCategory(dto.getCategory());
        if (dto.getPriority() != null) request.setPriority(dto.getPriority());
        if (dto.getDescription() != null) request.setDescription(dto.getDescription());
        if (dto.getLocation() != null) request.setLocation(dto.getLocation());
        
        if (dto.getDivisionId() != null) {
            DivisionRules.assertAllowed(dto.getDivisionId());
            request.setDivisionId(dto.getDivisionId());
        }

        return maintenanceRepository.save(request);
    }

    public List<MaintenanceRequest> search(UserPrincipal user,
                                           String status,
                                           String priority,
                                           String maintenanceId,
                                           String divisionId,
                                           String createdBy) { // Changed to String
        // Professionals only see their assigned requests (supports multiple professionals)
        if (user.getRole() == Role.PROFESSIONAL) {
            String professionalId = user.getId();
            System.out.println("=== PROFESSIONAL FETCHING MAINTENANCE ===");
            System.out.println("Professional ID: " + professionalId);
            System.out.println("Professional Email: " + user.getEmail());
            System.out.println("Professional Role: " + user.getRole());
            
            // Find all maintenance where this professional is in the assigned list
            List<MaintenanceRequest> allRequests = maintenanceRepository.findAll();
            List<MaintenanceRequest> assignedRequests = allRequests.stream()
                .filter(m -> m.isAssignedToProfessional(professionalId))
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("Found " + assignedRequests.size() + " maintenance requests assigned to professional");
            for (MaintenanceRequest m : assignedRequests) {
                System.out.println("  - Maintenance: " + m.getMaintenanceId() + ", Assigned to: " + m.getAssignedProfessionalIdsList());
            }
            
            return assignedRequests;
        }
        
        // Supervisors need a division
        if (user.getRole() == Role.SUPERVISOR && user.getDivisionId() == null) {
            throw new ApiException("Division not set for supervisor");
        }
        
        // Supervisors only see requests assigned to them OR in their division
        if (user.getRole() == Role.SUPERVISOR) {
            String supervisorId = user.getId();
            System.out.println("=== SUPERVISOR FETCHING MAINTENANCE ===");
            System.out.println("Supervisor ID: " + supervisorId);
            System.out.println("Supervisor Email: " + user.getEmail());
            System.out.println("Supervisor Division: " + user.getDivisionId());
            
            // Find by assigned supervisor ID OR by division
            // Normalize divisionId: Keycloak stores "1", DB stores "DIV-001"
            String normalizedDivisionId = DivisionRules.normalize(user.getDivisionId());
            List<MaintenanceRequest> assignedToMe = maintenanceRepository.findByAssignedSupervisorId(supervisorId);
            List<MaintenanceRequest> inMyDivision = maintenanceRepository.findByDivisionId(normalizedDivisionId);
            
            // Combine and deduplicate
            Set<MaintenanceRequest> combined = new HashSet<>(assignedToMe);
            combined.addAll(inMyDivision);
            
            System.out.println("Found " + assignedToMe.size() + " maintenance requests assigned to supervisor");
            System.out.println("Found " + inMyDivision.size() + " maintenance requests in supervisor's division");
            System.out.println("Total unique: " + combined.size());
            
            return new ArrayList<>(combined);
        }
        
        // Users only see their own requests
        if (user.getRole() == Role.USER) {
            return maintenanceRepository.findByCreatedBy(user.getId());
        }
        
        // Admin can search with filters
        Specification<MaintenanceRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Status.fromValue(status)));
            }
            if (priority != null && !priority.isBlank()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (maintenanceId != null && !maintenanceId.isBlank()) {
                predicates.add(cb.equal(root.get("maintenanceId"), maintenanceId));
            }
            if (divisionId != null) {
                predicates.add(cb.equal(root.get("divisionId"), divisionId));
            }
            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return maintenanceRepository.findAll(spec);
    }

    @Transactional
    public MaintenanceRequest adminAssignProfessional(Long id, String professionalId, String instructions, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        MaintenanceRequest maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Maintenance request not found"));
        
        System.out.println("=== ASSIGNING MAINTENANCE TO PROFESSIONAL ===");
        System.out.println("Maintenance ID: " + maintenance.getId());
        System.out.println("Maintenance Business ID: " + maintenance.getMaintenanceId());
        System.out.println("Professional ID: " + professionalId);
        System.out.println("Current Status: " + maintenance.getStatus());
        System.out.println("Currently Assigned: " + maintenance.getAssignedProfessionalIdsList());

        // For maintenance, admin can directly assign professional from Submitted or Under Review
        Status current = maintenance.getStatus();
        if (current == Status.SUBMITTED) {
            maintenance.setStatus(Status.UNDER_REVIEW);
            current = Status.UNDER_REVIEW;
        }
        if (current != Status.UNDER_REVIEW && current != Status.ASSIGNED_TO_PROFESSIONALS) {
            throw new ApiException("Maintenance must be under review before assigning a professional");
        }
        
        // Add to the list of assigned professionals (supports multiple)
        maintenance.addAssignedProfessional(professionalId);
        maintenance.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
        
        System.out.println("New Status: " + maintenance.getStatus());
        System.out.println("Assigned Professionals: " + maintenance.getAssignedProfessionalIdsList());
        System.out.println("Notification sent to professional");
        System.out.println("=== ASSIGNMENT COMPLETE ===");
        
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public void delete(Long id, UserPrincipal currentUser) {
        MaintenanceRequest maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Maintenance request not found"));
        
        // Users can only delete their own requests in Submitted status
        // Admins can delete any request
        if (currentUser.getRole() == Role.USER) {
            if (!maintenance.getCreatedBy().equals(currentUser.getId())) {
                throw new ApiException("You can only delete your own requests");
            }
            if (maintenance.getStatus() != Status.SUBMITTED) {
                throw new ApiException("You can only delete requests in Submitted status");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only users and admins can delete requests");
        }
        
        System.out.println("=== DELETING MAINTENANCE ===");
        System.out.println("Maintenance ID: " + maintenance.getId());
        System.out.println("Maintenance Business ID: " + maintenance.getMaintenanceId());
        System.out.println("Deleted by: " + currentUser.getUsername() + " (Role: " + currentUser.getRole() + ")");
        
        maintenanceRepository.delete(maintenance);
        
        System.out.println("=== MAINTENANCE DELETED ===");
    }

    public MaintenanceRequest adminApprove(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        MaintenanceRequest maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Maintenance request not found"));
        requestLifecycleService.transition(RequestType.MAINTENANCE, maintenance.getId(), Status.APPROVED, admin.getId());
        maintenance.setStatus(Status.APPROVED);
        requestLifecycleService.notifyUser(maintenance.getCreatedBy(), "Maintenance approved", "Maintenance request " + maintenance.getMaintenanceId() + " approved");
        return maintenanceRepository.save(maintenance);
    }

    public MaintenanceRequest adminReject(Long id, String reason, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        MaintenanceRequest maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Maintenance request not found"));
        requestLifecycleService.transition(RequestType.MAINTENANCE, maintenance.getId(), Status.REJECTED, admin.getId());
        maintenance.setStatus(Status.REJECTED);
        maintenance.setRejectionReason(reason);
        String notificationMessage = "Maintenance request " + maintenance.getMaintenanceId() + " rejected";
        if (reason != null && !reason.isBlank()) {
            notificationMessage += ". Reason: " + reason;
        }
        requestLifecycleService.notifyUser(maintenance.getCreatedBy(), "Maintenance rejected", notificationMessage);
        return maintenanceRepository.save(maintenance);
    }
}
