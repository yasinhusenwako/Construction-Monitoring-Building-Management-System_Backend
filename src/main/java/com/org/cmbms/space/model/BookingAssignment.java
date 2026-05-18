package com.org.cmbms.space.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_assignments")
@Getter
@Setter
public class BookingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;
    
    @Column(name = "professional_id", nullable = false, length = 255)
    private String professionalId;
    
    @Column(name = "instructions", nullable = false, columnDefinition = "TEXT")
    private String instructions;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
    
    @Column(name = "status", length = 50)
    private String status = "ACTIVE";
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
