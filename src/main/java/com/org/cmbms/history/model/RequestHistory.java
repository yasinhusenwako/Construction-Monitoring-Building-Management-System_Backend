package com.org.cmbms.history.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_history")
@Getter
@Setter
public class RequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long requestId;
    private String requestType; // PROJECT, MAINTENANCE, BOOKING
    private String action; // e.g., "Status Updated", "Assigned", "Note Added"
    private String status;
    private String actorName;
    private Long actorId;
    
    @Column(columnDefinition = "TEXT")
    private String note;
    
    private LocalDateTime createdAt;
}
