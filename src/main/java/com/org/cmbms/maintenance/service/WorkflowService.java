package com.org.cmbms.maintenance.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
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
        return maintenanceRepository.findByDivisionId(supervisor.getDivisionId());
    }

    public List<MaintenanceRequest> getProfessionalTasks(UserPrincipal professional) {
        ensureRole(professional, Role.PROFESSIONAL);
        return maintenanceRepository.findByAssignedProfessionalId(professional.getId());
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
        MaintenanceRequest maintenance = getMaintenance(request.getRequestId());
        maintenance.setDivisionId(request.getDivisionId());
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            maintenance.setPriority(request.getPriority());
        }

        User supervisor = userRepository.findById(request.getSupervisorId())
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found"));
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new ApiException("Selected user is not a supervisor");
        }
        maintenance.setAssignedSupervisorId(supervisor.getId());
        transition(maintenance, Status.UNDER_REVIEW, admin.getId());
        transition(maintenance, Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        requestLifecycleService.notifyUser(supervisor.getId(), "New assignment", "You have been assigned maintenance " + maintenance.getMaintenanceId());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest assignProfessional(UserPrincipal supervisor, AssignProfessionalRequest request) {
        ensureRole(supervisor, Role.SUPERVISOR);
        MaintenanceRequest maintenance = getMaintenance(request.getRequestId());
        if (!maintenance.getDivisionId().equals(supervisor.getDivisionId())) {
            throw new ApiException("supervisor sees only division requests");
        }
        User professional = userRepository.findById(request.getAssignedProfessionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Selected user is not a professional");
        }
        if (professional.getDivisionId() == null || !professional.getDivisionId().equals(supervisor.getDivisionId())) {
            throw new ApiException("Professional must belong to the same division as the supervisor and request.");
        }

        maintenance.setAssignedProfessionalId(professional.getId());
        transition(maintenance, Status.ASSIGNED_TO_PROFESSIONALS, supervisor.getId());

        WorkOrder workOrder = workOrderRepository.findByMaintenanceRequestId(maintenance.getId()).orElse(new WorkOrder());
        workOrder.setMaintenanceRequestId(maintenance.getId());
        workOrder.setAssignedProfessionalId(professional.getId());
        workOrder.setInstructions(request.getInstructions());
        workOrder.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
        workOrderRepository.save(workOrder);

        requestLifecycleService.notifyUser(professional.getId(), "New task", "Maintenance " + maintenance.getMaintenanceId() + " assigned to you");
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest updateTaskStatus(UserPrincipal professional, Long id, TaskStatusUpdateRequest request) {
        ensureRole(professional, Role.PROFESSIONAL);
        MaintenanceRequest maintenance = getMaintenance(id);
        if (!professional.getId().equals(maintenance.getAssignedProfessionalId())) {
            throw new ApiException("professional sees only assigned tasks");
        }
        if (request.getStatus() != Status.IN_PROGRESS && request.getStatus() != Status.COMPLETED) {
            throw new ApiException("Professional can only update to IN_PROGRESS or COMPLETED");
        }
        transition(maintenance, request.getStatus(), professional.getId());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminApprove(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        transition(maintenance, Status.APPROVED, admin.getId());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminReject(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        transition(maintenance, Status.REJECTED, admin.getId());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminStartReview(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        // Move to UNDER_REVIEW to start the admin review process
        transition(maintenance, Status.UNDER_REVIEW, admin.getId());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest adminClose(UserPrincipal admin, Long requestId) {
        ensureRole(admin, Role.ADMIN);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        transition(maintenance, Status.CLOSED, admin.getId());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public MaintenanceRequest supervisorReview(UserPrincipal supervisor, Long requestId) {
        ensureRole(supervisor, Role.SUPERVISOR);
        MaintenanceRequest maintenance = getMaintenance(requestId);
        if (!supervisor.getDivisionId().equals(maintenance.getDivisionId())) {
            throw new ApiException("supervisor sees only division requests");
        }
        transition(maintenance, Status.REVIEWED, supervisor.getId());
        return maintenanceRepository.save(maintenance);
    }

    public void transition(MaintenanceRequest request, Status next, Long changedBy) {
        Status current = request.getStatus();
        if (current == null) {
            if (next != Status.SUBMITTED) {
                throw new ApiException("First status must be SUBMITTED");
            }
        } else {
            EnumSet<Status> allowed = TRANSITIONS.get(current);
            if (allowed == null || !allowed.contains(next)) {
                // Allow admins to force transition if needed (relax for admin assignment)
                if (next == Status.ASSIGNED_TO_PROFESSIONALS || next == Status.ASSIGNED_TO_SUPERVISOR) {
                    // Log a warning or handle as needed, but allow transition
                } else {
                    throw new ApiException("Invalid transition: " + current + " -> " + next);
                }
            }
        }
        request.setStatus(next);
        StatusHistory history = new StatusHistory();
        history.setRequestId(request.getId());
        history.setRequestType(RequestType.MAINTENANCE);
        history.setStatus(next);
        history.setChangedBy(changedBy);
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);
        if (next == Status.COMPLETED && request.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(request.getAssignedSupervisorId(), "Task completed", "Maintenance " + request.getMaintenanceId() + " completed");
        }
        if (next == Status.REVIEWED) {
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                requestLifecycleService.notifyUser(admin.getId(), "Supervisor reviewed", "Maintenance " + request.getMaintenanceId() + " reviewed");
            }
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

    private MaintenanceRequest getMaintenance(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));
    }

    private void ensureRole(UserPrincipal principal, Role role) {
        if (principal.getRole() != role) {
            throw new ApiException("Access denied");
        }
    }

}

