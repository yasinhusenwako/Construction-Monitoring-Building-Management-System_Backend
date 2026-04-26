package com.org.cmbms.preventive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "preventive_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreventiveSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String scheduleId; // PM-001, PM-002, etc.

    @Column(nullable = false)
    private String system; // HVAC – Floor 1 & 2, Elevator A1, etc.

    @Column(nullable = false)
    private String frequency; // Every 3 months, Every 6 months, etc.

    @Column(nullable = false)
    private LocalDate lastDone;

    @Column(nullable = false)
    private LocalDate nextDue;

    @Column(nullable = false)
    private String status; // Scheduled, Due Soon, Due Today, Overdue

    @Column(nullable = false)
    private String assignee; // Technician name

    @Column(nullable = false)
    private Long assignedProfessionalId; // User ID of assigned professional

    private String notes;

    @Column(nullable = false)
    private LocalDate createdAt;

    @Column(nullable = false)
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
        updateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
        updateStatus();
    }

    private void updateStatus() {
        LocalDate today = LocalDate.now();
        if (nextDue.isBefore(today)) {
            status = "Overdue";
        } else if (nextDue.isEqual(today)) {
            status = "Due Today";
        } else if (nextDue.isBefore(today.plusDays(7))) {
            status = "Due Soon";
        } else {
            status = "Scheduled";
        }
    }
}
