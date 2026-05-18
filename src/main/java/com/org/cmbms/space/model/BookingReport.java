package com.org.cmbms.space.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_reports")
@Getter
@Setter
public class BookingReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;
    
    @Column(name = "report_text", nullable = false, columnDefinition = "TEXT")
    private String reportText;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
    
    @Column(name = "viewed", nullable = false)
    private boolean viewed = false;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
