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
    private String assignedProfessionalId; // DEPRECATED: Use assignedProfessionalIds instead
    
    @Column(name = "assignedProfessionalIds", columnDefinition = "TEXT")
    private String assignedProfessionalIds; // Comma-separated list of professional IDs (emails or numeric IDs)
    
    @Column(name = "materialCost")
    private Double materialCost;
    @Column(name = "laborCost")
    private Double laborCost;
    @Column(name = "totalCost")
    private Double totalCost;
    @Column(name = "partsUsed", columnDefinition = "TEXT")
    private String partsUsed;
    
    // Helper methods for multiple professional assignment
    public java.util.List<String> getAssignedProfessionalIdsList() {
        if (assignedProfessionalIds == null || assignedProfessionalIds.trim().isEmpty()) {
            // Fallback to single professional for backward compatibility
            if (assignedProfessionalId != null && !assignedProfessionalId.trim().isEmpty()) {
                return java.util.Arrays.asList(assignedProfessionalId);
            }
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(assignedProfessionalIds.split(","));
    }
    
    public void setAssignedProfessionalIdsList(java.util.List<String> professionalIds) {
        if (professionalIds == null || professionalIds.isEmpty()) {
            this.assignedProfessionalIds = null;
            this.assignedProfessionalId = null;
        } else {
            this.assignedProfessionalIds = String.join(",", professionalIds);
            // Keep first professional in old field for backward compatibility
            this.assignedProfessionalId = professionalIds.get(0);
        }
    }
    
    public void addAssignedProfessional(String professionalId) {
        java.util.List<String> current = new java.util.ArrayList<>(getAssignedProfessionalIdsList());
        if (!current.contains(professionalId)) {
            current.add(professionalId);
            setAssignedProfessionalIdsList(current);
        }
    }
    
    public void removeAssignedProfessional(String professionalId) {
        java.util.List<String> current = new java.util.ArrayList<>(getAssignedProfessionalIdsList());
        current.remove(professionalId);
        setAssignedProfessionalIdsList(current);
    }
    
    public boolean isAssignedToProfessional(String professionalId) {
        return getAssignedProfessionalIdsList().contains(professionalId);
    }
}

