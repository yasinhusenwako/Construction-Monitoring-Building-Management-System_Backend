package com.org.cmbms.workflow.service;

import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.maintenance.model.StatusHistory;
import com.org.cmbms.maintenance.repository.StatusHistoryRepository;
import com.org.cmbms.notification.model.Notification;
import com.org.cmbms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequestLifecycleService {

    // Maintenance workflow: requires supervisor assignment
    private static final Map<Status, EnumSet<Status>> MAINTENANCE_TRANSITIONS = Map.of(
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

    // Project/Booking workflow: admin can directly assign professional
    private static final Map<Status, EnumSet<Status>> PROJECT_BOOKING_TRANSITIONS = Map.of(
            Status.SUBMITTED, EnumSet.of(Status.UNDER_REVIEW),
            Status.UNDER_REVIEW, EnumSet.of(Status.ASSIGNED_TO_PROFESSIONALS),
            Status.ASSIGNED_TO_PROFESSIONALS, EnumSet.of(Status.IN_PROGRESS),
            Status.IN_PROGRESS, EnumSet.of(Status.COMPLETED),
            Status.COMPLETED, EnumSet.of(Status.APPROVED, Status.REJECTED),
            Status.APPROVED, EnumSet.of(Status.CLOSED),
            Status.REJECTED, EnumSet.of(Status.CLOSED)
    );

    private final StatusHistoryRepository statusHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final com.org.cmbms.history.service.RequestHistoryService requestHistoryService;
    private final com.org.cmbms.user.repository.UserRepository userRepository;
    private final com.org.cmbms.user.service.KeycloakAdminService keycloakAdminService;

    private Map<Status, EnumSet<Status>> getTransitions(RequestType type) {
        if (type == RequestType.MAINTENANCE) {
            return MAINTENANCE_TRANSITIONS;
        }
        return PROJECT_BOOKING_TRANSITIONS;
    }

    @Transactional
    public void initialize(RequestType type, Long requestId, String changedBy) {
        // Try to parse changedBy as Long for database users
        Long numericChangedBy = null;
        try {
            numericChangedBy = Long.parseLong(changedBy);
        } catch (NumberFormatException e) {
            // Keycloak user (email-based ID) - use 0 as placeholder
            numericChangedBy = 0L;
        }
        
        StatusHistory history = new StatusHistory();
        history.setRequestId(requestId);
        history.setRequestType(type);
        history.setStatus(Status.SUBMITTED);
        history.setChangedBy(numericChangedBy);
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);
        recordRequestHistory(requestId, type, Status.SUBMITTED, numericChangedBy, "Request Created", null);
    }
    
    // Overload for backward compatibility with Long changedBy
    @Transactional
    public void initialize(RequestType type, Long requestId, Long changedBy) {
        initialize(type, requestId, String.valueOf(changedBy));
    }

    @Transactional
    public void initializeProjectLifecycle(Long projectId, String role, String businessId) {
        StatusHistory history = new StatusHistory();
        history.setRequestId(projectId);
        history.setRequestType(RequestType.PROJECT);
        history.setStatus(Status.SUBMITTED);
        history.setChangedBy(0L); // System/USER
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);
        
        recordRequestHistory(projectId, RequestType.PROJECT, Status.SUBMITTED, 0L, "Request Created", null);
    }

    @Transactional
    public void initializeSpaceLifecycle(Long bookingId, String role, String businessId) {
        StatusHistory history = new StatusHistory();
        history.setRequestId(bookingId);
        history.setRequestType(RequestType.BOOKING);
        history.setStatus(Status.SUBMITTED);
        history.setChangedBy(0L); // System/USER
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);

        recordRequestHistory(bookingId, RequestType.BOOKING, Status.SUBMITTED, 0L, "Request Created", null);
    }

    @Transactional
    public void transition(RequestType type, Long requestId, Status next, Long changedBy) {
        Status current = getCurrentStatus(type, requestId);
        if (current == null) {
            if (next != Status.SUBMITTED) {
                throw new ApiException("First status must be Submitted");
            }
        } else {
            Map<Status, EnumSet<Status>> transitions = getTransitions(type);
            EnumSet<Status> allowed = transitions.get(current);
            if (allowed == null || !allowed.contains(next)) {
                throw new ApiException("Invalid transition: " + current.getValue() + " -> " + next.getValue());
            }
        }
        StatusHistory history = new StatusHistory();
        history.setRequestId(requestId);
        history.setRequestType(type);
        history.setStatus(next);
        history.setChangedBy(changedBy);
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);

        recordRequestHistory(requestId, type, next, changedBy, next.getValue(), null);
    }
    
    // Overload for String changedBy (Keycloak users)
    @Transactional
    public void transition(RequestType type, Long requestId, Status next, String changedBy) {
        Long numericChangedBy = null;
        try {
            numericChangedBy = Long.parseLong(changedBy);
        } catch (NumberFormatException e) {
            numericChangedBy = 0L; // Keycloak user placeholder
        }
        transition(type, requestId, next, numericChangedBy);
    }

    public void recordNote(RequestType type, Long requestId, Long actorId, String note) {
        Status current = getCurrentStatus(type, requestId);
        recordRequestHistory(requestId, type, current, actorId, "Note Added", note);
    }
    
    // Overload for String actorId (Keycloak users)
    public void recordNote(RequestType type, Long requestId, String actorId, String note) {
        Long numericActorId = null;
        try {
            numericActorId = Long.parseLong(actorId);
        } catch (NumberFormatException e) {
            numericActorId = 0L; // Keycloak user placeholder
        }
        recordNote(type, requestId, numericActorId, note);
    }

    private void recordRequestHistory(Long requestId, RequestType type, Status status, Long changedBy, String action, String note) {
        String actorName = "System";
        if (changedBy != null && changedBy > 0) {
            actorName = userRepository.findById(changedBy)
                    .map(com.org.cmbms.user.model.User::getName)
                    .orElse("Unknown User");
        }
        requestHistoryService.recordHistory(
                requestId,
                type.name(),
                action,
                status != null ? status.getValue() : null,
                actorName,
                changedBy,
                note
        );
    }

    public Status getCurrentStatus(RequestType type, Long requestId) {
        List<StatusHistory> history = statusHistoryRepository.findByRequestTypeAndRequestIdOrderByTimestampAsc(type, requestId);
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1).getStatus();
    }

    public void notifyUser(String userId, String title, String message) {
        // Unified notification system for both database and Keycloak users
        // userId can be either numeric ID (database users) or email (Keycloak users)
        
        String finalUserId = userId;
        
        // If it's a numeric ID, try to find the user's email (for Keycloak users)
        try {
            Long numericId = Long.parseLong(userId);
            // It's numeric - check if this is a database user or if we need to find Keycloak email
            com.org.cmbms.user.model.User dbUser = userRepository.findById(numericId).orElse(null);
            if (dbUser != null && dbUser.getEmail() != null) {
                // Database user exists - check if they also exist in Keycloak
                // For now, send notification to both numeric ID and email
                // This ensures backward compatibility
                
                // Send to numeric ID (for database users)
                Notification notification1 = new Notification();
                notification1.setUserId(String.valueOf(numericId));
                notification1.setTitle(title);
                notification1.setMessage(message);
                notification1.setIsRead(false);
                notification1.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notification1);
                
                // Also send to email (for Keycloak users with same email)
                Notification notification2 = new Notification();
                notification2.setUserId(dbUser.getEmail());
                notification2.setTitle(title);
                notification2.setMessage(message);
                notification2.setIsRead(false);
                notification2.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notification2);
                
                System.out.println("📧 Notification sent to user: " + numericId + " (and " + dbUser.getEmail() + ") - " + title);
                return;
            }
        } catch (NumberFormatException e) {
            // It's already an email - use as-is
        }
        
        // Send notification with the provided userId (email or numeric)
        Notification notification = new Notification();
        notification.setUserId(finalUserId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        System.out.println("📧 Notification sent to user: " + finalUserId + " - " + title);
    }
    
    // Overload for backward compatibility with Long userId
    public void notifyUser(Long userId, String title, String message) {
        notifyUser(String.valueOf(userId), title, message);
    }
    
    /**
     * Notify all users with a specific role (both Keycloak and database users)
     */
    public void notifyUsersByRole(com.org.cmbms.common.enums.Role role, String title, String message) {
        // Notify database users with this role
        List<com.org.cmbms.user.model.User> dbUsers = userRepository.findByRole(role);
        for (com.org.cmbms.user.model.User user : dbUsers) {
            notifyUser(String.valueOf(user.getId()), title, message);
        }
        
        // Notify Keycloak users with this role
        try {
            List<org.keycloak.representations.idm.UserRepresentation> keycloakUsers = 
                keycloakAdminService.getUsersByRole(role.name());
            for (org.keycloak.representations.idm.UserRepresentation user : keycloakUsers) {
                // Use email as user ID for Keycloak users
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    notifyUser(user.getEmail(), title, message);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to notify Keycloak users with role " + role + ": " + e.getMessage());
        }
    }
}

