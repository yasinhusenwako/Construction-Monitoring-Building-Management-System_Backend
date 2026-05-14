package com.org.cmbms.notification.repository;

import com.org.cmbms.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);  // Changed from Long to String
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}

