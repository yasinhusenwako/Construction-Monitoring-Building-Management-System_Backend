package com.org.cmbms.notification.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.notification.model.Notification;
import com.org.cmbms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        // Ensure user can only mark their own notifications as read
        if (!notification.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        notification.setIsRead(Boolean.TRUE);
        return ResponseEntity.ok(notificationRepository.save(notification));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(n -> n.setIsRead(Boolean.TRUE));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok().build();
    }
}

