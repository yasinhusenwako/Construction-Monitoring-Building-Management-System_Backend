package com.org.cmbms.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", length = 255)
    private String userId;  // Changed from Long to String to support both numeric IDs and emails
    
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private Boolean isRead;
    
    private LocalDateTime createdAt;
}


