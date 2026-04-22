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

    private Map<Status, EnumSet<Status>> getTransitions(RequestType type) {
        if (type == RequestType.MAINTENANCE) {
            return MAINTENANCE_TRANSITIONS;
        }
        return PROJECT_BOOKING_TRANSITIONS;
    }

    @Transactional
    public void initialize(RequestType type, Long requestId, Long changedBy) {
        StatusHistory history = new StatusHistory();
        history.setRequestId(requestId);
        history.setRequestType(type);
        history.setStatus(Status.SUBMITTED);
        history.setChangedBy(changedBy);
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);
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
    }

    public Status getCurrentStatus(RequestType type, Long requestId) {
        List<StatusHistory> history = statusHistoryRepository.findByRequestTypeAndRequestId(type, requestId);
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1).getStatus();
    }

    public void notifyUser(Long userId, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}

