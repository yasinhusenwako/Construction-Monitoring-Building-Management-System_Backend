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
    private Long userId;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

