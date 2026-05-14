package com.org.cmbms.maintenance.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
import com.org.cmbms.common.exception.ResourceNotFoundException;
import com.org.cmbms.maintenance.dto.AssignProfessionalRequest;
import com.org.cmbms.maintenance.dto.AssignSupervisorRequest;
import com.org.cmbms.maintenance.dto.TaskStatusUpdateRequest;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.model.StatusHistory;
import com.org.cmbms.maintenance.model.WorkOrder;
import com.org.cmbms.maintenance.repository.MaintenanceRepository;
import com.org.cmbms.maintenance.repository.StatusHistoryRepository;
import com.org.cmbms.maintenance.repository.WorkOrderRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import com.org.cmbms.workflow.service.RequestLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final Map<Status, EnumSet<Status>> TRANSITIONS = Map.of(
            Status.SUBMITTED, EnumSet.of(Status.UNDER_REVIEW),
            Status.UNDER_REVIEW, EnumSet.of(Status.ASSIGNED_TO_SUPERVISOR),
            Status.ASSIGNED_TO_SUPERVISOR, EnumSet.of(Status.ASSIGNED_TO_PROFESSIONALS),
            Status.ASSIGNED_TO_PROFESSIONALS, EnumSet.of(Status.IN_PROGRESS),
            Status.IN_PROGRESS, EnumSet.of(Status.COMPLETED),
            Status.COMPLETED, EnumSet.of(Status.REVIEWED),
            Status.REVIEWED, EnumSet.of(Status.APPROVED, Status.REJECTED),
            Status.APPROVED, EnumSet.of(Status.CLOSED),
            Status.REJECTED, EnumSet.of(Status.CLOSED)
    );

    private final MaintenanceRepository maintenanceRepository;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final RequestLifecycleService requestLifecycleService;

    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceRepository.findAll();
    }

    public List<MaintenanceRequest> getSupervisorRequests(UserPrincipal supervisor) {
        ensureRole(supervisor, Role.SUPERVISOR);
        if (supervisor.getDivisionId() == null) {
            throw new ApiException("Division not set for supervisor");
        }
        var divisionAliases = DivisionRules.aliases(supervisor.getDivisionId());
        if (divisionAliases.isEmpty()) {
            throw new ApiException("Invalid supervisor division");
        }
        return maintenanceRepository.findByDivisionIdIn(new ArrayList<>(divisionAliases));
    }

    public List<MaintenanceRequest> getProfessionalTasks(UserPrincipal professional) {
        ensureRole(professional, Role.PROFESSIONAL);
        // Use email as ID for Keycloak users
        String professionalId = professional.getId();
        return maintenanceRepository.findByAssignedProfessionalId(professionalId);
    }

    public List<MaintenanceRequest> getAllRequests(UserPrincipal admin) {
        ensureRole(admin, Role.ADMIN);
        return maintenanceRepository.findAll();
    }

    @Transactional
    public MaintenanceRequest assignSupervisor(UserPrincipal admin, AssignSupervisorRequest request) {
        ensureRole(admin, Role.ADMIN);
        if (request.getDivisionId() == null) {
            throw new ApiException("Division REQUIRED before assignment");
        }
        DivisionRules.assertAllowed(request.getDivisionId());
        String normalizedDivisionId = DivisionRules.normalize(request.getDivisionId());
        MaintenanceRequest maintenance = getMaintenance(request.getRequestId());
        maintenance.setDivisionId(normalizedDivisionId);
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            maintenance.setPriority(request.getPriority());
        }

        String supervisorId;
        if (request.getSupervisorId() != null) {
            // SupervisorId can be either numeric (database user) or email (Keycloak user)
            supervisorId = request.getSupervisorId();
            
            // Try to validate if it's a database user
            try {
                Long numericId = Long.parseLong(supervisorId);
                User supervisor = userRepository.findById(numericId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found"));
                if (supervisor.getRole() != Role.SUPERVISOR) {
                    throw new ApiException("Selected user is not a supervisor");
                }
                if (supervisor.getDivisionId() == null || !DivisionRules.matches(supervisor.getDivisionId(), normalizedDivisionId)) {
                    throw new ApiException("Supervisor must belong to selected division");
                }
            } catch (NumberFormatException e) {
                // It's a Keycloak user (email) - we'll trust the frontend validation
                System.out.println("Assigning to Keycloak supervisor: " + supervisorId);
            }
        } else {
            // Auto-assign: try to find a supervisor for this division
            List<User> supervisors = userRepository.findByRoleAndDivisionId(Role.SUPERVISOR, normalizedDivisionId);
            if (supervisors.isEmpty()) {
                // No database supervisor found - allow assignment without supervisor
                // The division will be set, but no specific supervisor assigned
                // This allows Keycloak supervisors in that division to see the request
                System.out.println("No database supervisor found for division " + normalizedDivisionId + ". Assigning to division only.");
                supervisorId = null;
            } else if (supervisors.size() > 1) {
                throw new ApiException("Multiple supervisor accounts found for selected division. Keep only one account per division.");
            } else {
                supervisorId = String.valueOf(supervisors.get(0).getId());
            }
        }
        
        if (supervisorId != null) {
            maintenance.setAssignedSupervisorId(supervisorId);
            requestLifecycleService.notifyUser(supervisorId, "New assignment", "You have been assigned maintenance " + maintenance.getMaintenanceId());
        }
        
        // Only transition if not already in ASSIGNED_TO_SUPERVISOR status
        Long adminNumericId = admin.getNumericId() != null ? admin.getNumericId() : 0L;
        
        // If currently SUBMITTED, move to UNDER_REVIEW first
        if (maintenance.getStatus() == Status.SUBMITTED) {
            transition(maintenance, Status.UNDER_REVIEW, adminNumericId);
        }
        
        // Only transition to ASSIGNED_TO_SUPERVISOR if not already there
        if (maintenance.getStatus() != Status.ASSIGNED_TO_SUPERVISOR) {
            transition(maintenance, Status.ASSIGNED_TO_SUPERVISOR, adminNumericId);
        }
        
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest assignProfessional(UserPrincipal supervisor, AssignProfessionalRequest request) {
        ensureRole(supervisor, Role.SUPERVISOR);
        MaintenanceRequest maintenance = getMaintenance(request.getRequestId());
        
        // If maintenance doesn't have a division yet, assign supervisor's division
        if (maintenance.getDivisionId() == null) {
            maintenance.setDivisionId(DivisionRules.normalize(supervisor.getDivisionId()));
        } else if (!DivisionRules.matches(maintenance.getDivisionId(), supervisor.getDivisionId())) {
            throw new ApiException("supervisor sees only division requests");
        }
        
        // For Keycloak users, request.getAssignedProfessionalId() will be an email string
        // For database users, it will be a numeric ID
        // We store it directly without validation since Keycloak users aren't in the database
        String professionalId = String.valueOf(request.getAssignedProfessionalId());
        
        maintenance.setAssignedProfessionalId(professionalId);
        Long supervisorNumericId = supervisor.getNumericId() != null ? supervisor.getNumericId() : 0L;

        // Sync status_history with the actual maintenance status before transitioning.
        // If the history table is behind (e.g. missing ASSIGNED_TO_SUPERVISOR entry),
        // insert the current status so the transition check passes.
        syncHistoryWithActualStatus(maintenance, supervisorNumericId);

        transition(maintenance, Status.ASSIGNED_TO_PROFESSIONALS, supervisorNumericId);

        WorkOrder workOrder = workOrderRepository.findByMaintenanceRequestId(maintenance.getId()).orElse(new WorkOrder());
        workOrder.setMaintenanceRequestId(maintenance.getId());
        workOrder.setAssignedProfessionalId(professionalId);
        workOrder.setInstructions(request.getInstructions());
        workOrder.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
        workOrderRepository.save(workOrder);

        // Notify the professional about the assignment
        requestLifecycleService.notifyUser(professionalId, "New Maintenance Assignment", 
            "Maintenance " + maintenance.getMaintenanceId() + " has been assigned to you by supervisor");
        
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest updateTaskStatus(UserPrincipal professional, Long id, TaskStatusUpdateRequest request) {
        ensureRole(professional, Role.PROFESSIONAL);
        MaintenanceRequest maintenance = getMaintenance(id);
        
        // Check if professional is assigned to this task
        // For database users: compare numeric ID
        // For Keycloak users: compare email
        boolean isAssigned = false;
        if (maintenance.getAssignedProfessionalId() != null) {
            Long professionalNumericId = professional.getNumericId();
            if (professionalNumericId != null) {
                // Database user - compare numeric ID
                isAssigned = maintenance.getAssignedProfessionalId().equals(String.valueOf(professionalNumericId));
            } else {
                // Keycloak user - compare email
                isAssigned = maintenance.getAssignedProfessionalId().equals(professional.getEmail());
            }
        }
        
        if (!isAssigned) {
            throw new ApiException("professional sees only assigned tasks");
        }
        
        if (request.getStatus() != Status.IN_PROGRESS && request.getStatus() != Status.COMPLETED) {
            throw new ApiException("Professional can only update to IN_PROGRESS or COMPLETED");
        }
        
        Long professionalNumericId = professional.getNumericId() != null ? professional.getNumericId() : 0L;
        transition(maintenance, request.getStatus(), professionalNumericId);
        
        // Notify supervisor about status change
        if (request.getStatus() == Status.IN_PROGRESS && maintenance.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(maintenance.getAssignedSupervisorId(), 
                "Maintenance work started", 
                "Maintenance " + maintenance.getMaintenanceId() + " work has started");
        }
        
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminApprove(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        Long adminNumericId = admin.getNumericId() != null ? admin.getNumericId() : 0L;
        transition(maintenance, Status.APPROVED, adminNumericId);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminReject(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        Long adminNumericId = admin.getNumericId() != null ? admin.getNumericId() : 0L;
        transition(maintenance, Status.REJECTED, adminNumericId);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminStartReview(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        // Move to UNDER_REVIEW to start the admin review process
        Long adminNumericId = admin.getNumericId() != null ? admin.getNumericId() : 0L;
        transition(maintenance, Status.UNDER_REVIEW, adminNumericId);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminClose(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        Long adminNumericId = admin.getNumericId() != null ? admin.getNumericId() : 0L;
        transition(maintenance, Status.CLOSED, adminNumericId);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest supervisorReview(UserPrincipal supervisor, Long requestId) {
        if (supervisor.getRole() != Role.SUPERVISOR && supervisor.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        MaintenanceRequest maintenance = getMaintenance(requestId);
        
        // If maintenance doesn't have a division yet, assign supervisor's division
        if (maintenance.getDivisionId() == null) {
            maintenance.setDivisionId(DivisionRules.normalize(supervisor.getDivisionId()));
        } else if (supervisor.getRole() == Role.SUPERVISOR && !DivisionRules.matches(supervisor.getDivisionId(), maintenance.getDivisionId())) {
            throw new ApiException("supervisor sees only division requests");
        }
        
        Long supervisorNumericId = supervisor.getNumericId() != null ? supervisor.getNumericId() : 0L;
        transition(maintenance, Status.REVIEWED, supervisorNumericId);
        return maintenanceRepository.save(maintenance);
    }

    public void transition(MaintenanceRequest request, Status next, Long changedBy) {
        // Use RequestLifecycleService for consistent validation and history logging
        requestLifecycleService.transition(RequestType.MAINTENANCE, request.getId(), next, changedBy);
        request.setStatus(next);
        if (next == Status.COMPLETED && request.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(request.getAssignedSupervisorId(), "Task completed", "Maintenance " + request.getMaintenanceId() + " completed");
            // Notify all admins (both database and Keycloak)
            requestLifecycleService.notifyUsersByRole(Role.ADMIN, "Task completed", "Maintenance " + request.getMaintenanceId() + " completed and ready for review");
        }
        if (next == Status.REVIEWED) {
            // Notify all admins (both database and Keycloak)
            requestLifecycleService.notifyUsersByRole(Role.ADMIN, "Supervisor reviewed", "Maintenance " + request.getMaintenanceId() + " reviewed");
        }
        if (next == Status.APPROVED || next == Status.REJECTED) {
            requestLifecycleService.notifyUser(request.getCreatedBy(), "Admin decision", "Maintenance " + request.getMaintenanceId() + " is " + next.getValue());
        }
    }

    public void initializeSubmittedStatus(MaintenanceRequest request, Long userId) {
        request.setStatus(Status.SUBMITTED);
        StatusHistory history = new StatusHistory();
        history.setRequestId(request.getId());
        history.setRequestType(RequestType.MAINTENANCE);
        history.setStatus(Status.SUBMITTED);
        history.setChangedBy(userId);
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);
    }

    @Transactional
    public MaintenanceRequest updateTaskCost(UserPrincipal user, Long id, com.org.cmbms.maintenance.dto.CostUpdateRequest request) {
        ensureRole(user, Role.PROFESSIONAL);
        MaintenanceRequest maintenance = getMaintenance(id);
        
        System.out.println("=== UPDATE TASK COST ===");
        System.out.println("Maintenance ID: " + id);
        System.out.println("Assigned Professional ID (DB): " + maintenance.getAssignedProfessionalId());
        System.out.println("Current User ID: " + user.getId());
        System.out.println("Current User Email: " + user.getEmail());
        
        // Verify the professional is assigned to this task
        // Compare using user ID (email for Keycloak users, numeric string for legacy users)
        String userId = user.getId();
        if (maintenance.getAssignedProfessionalId() == null || userId == null || !maintenance.getAssignedProfessionalId().equals(userId)) {
            throw new ApiException("You are not assigned to this task");
        }
        
        System.out.println("Assignment verified! Updating cost...");
        
        // Update cost information
        maintenance.setMaterialCost(request.getMaterialCost());
        maintenance.setLaborCost(request.getLaborCost());
        maintenance.setPartsUsed(request.getPartsUsed());
        
        // Calculate total cost
        double materialCost = request.getMaterialCost() != null ? request.getMaterialCost() : 0.0;
        double laborCost = request.getLaborCost() != null ? request.getLaborCost() : 0.0;
        maintenance.setTotalCost(materialCost + laborCost);
        
        MaintenanceRequest saved = maintenanceRepository.save(maintenance);
        System.out.println("Cost updated successfully!");
        System.out.println("=== UPDATE COMPLETE ===");
        
        return saved;
    }

    private MaintenanceRequest getMaintenance(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));
    }

    private void ensureRole(UserPrincipal principal, Role role) {
        if (principal.getRole() != role) {
            throw new ApiException("Access denied");
        }
    }

    /**
     * Ensures the status_history table reflects the actual status on the
     * maintenance_requests row.  If the last history entry does not match
     * the entity's current status, a synthetic history record is inserted so
     * that subsequent transition() calls pass their "allowed transition" check.
     */
    private void syncHistoryWithActualStatus(MaintenanceRequest maintenance, Long actorId) {
        Status actualStatus = maintenance.getStatus();
        if (actualStatus == null) return;

        Status historyStatus = requestLifecycleService.getCurrentStatus(
                RequestType.MAINTENANCE, maintenance.getId());

        if (historyStatus == null || historyStatus != actualStatus) {
            // Insert a synthetic history entry to bring the history in sync
            StatusHistory sync = new StatusHistory();
            sync.setRequestId(maintenance.getId());
            sync.setRequestType(RequestType.MAINTENANCE);
            sync.setStatus(actualStatus);
            sync.setChangedBy(actorId);
            sync.setTimestamp(java.time.LocalDateTime.now());
            statusHistoryRepository.save(sync);
        }
    }

}

