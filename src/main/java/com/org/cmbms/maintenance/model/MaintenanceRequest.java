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
    private Long createdBy;
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    @Column(name = "divisionId")
    private Long divisionId;
    @Column(name = "assignedSupervisorId")
    private Long assignedSupervisorId;
    @Column(name = "assignedProfessionalId")
    private Long assignedProfessionalId;
    @Column(name = "materialCost")
    private Double materialCost;
    @Column(name = "laborCost")
    private Double laborCost;
    @Column(name = "totalCost")
    private Double totalCost;
    @Column(name = "partsUsed", columnDefinition = "TEXT")
    private String partsUsed;
}

