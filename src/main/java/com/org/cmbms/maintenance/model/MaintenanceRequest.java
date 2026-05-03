package com.org.cmbms.maintenance.model;

import com.org.cmbms.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests")
@Getter
@Setter
public class MaintenanceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "maintenanceId")
    private String maintenanceId;
    @Column(name = "category")
    private String category;
    @Column(name = "priority")
    private String priority;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "location")
    private String location;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    @Column(name = "rejectionReason", columnDefinition = "TEXT")
    private String rejectionReason;
    @Column(name = "createdBy")
    private String createdBy; // Changed to String to support both numeric IDs and email identifiers
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    @Column(name = "divisionId")
    private String divisionId; // Changed to String to support "DIV-001" format
    
    @Column(name = "assignedSupervisorId", length = 255)
    private String assignedSupervisorId; // Changed to String to support both numeric IDs and email identifiers
    
    @Column(name = "assignedProfessionalId")
    private String assignedProfessionalId; // Changed to String to support both numeric IDs and email identifiers
    @Column(name = "materialCost")
    private Double materialCost;
    @Column(name = "laborCost")
    private Double laborCost;
    @Column(name = "totalCost")
    private Double totalCost;
    @Column(name = "partsUsed", columnDefinition = "TEXT")
    private String partsUsed;
}

