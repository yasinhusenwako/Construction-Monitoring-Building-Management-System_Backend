
package com.org.cmbms.space.model;

import com.org.cmbms.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "bookingId")
    private String bookingId;
    @Column(name = "type")
    private String type;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    @Column(name = "rejectionReason", columnDefinition = "TEXT")
    private String rejectionReason;
    @Column(name = "requester")
    private String requester; // Changed to String to support both numeric IDs and email identifiers
    @Column(name = "dateTime")
    private LocalDateTime dateTime;
    @Column(name = "endTime")
    private LocalDateTime endTime;
    @Column(name = "capacity")
    private Integer capacity;
    @Column(name = "layout")
    private String layout;
    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;
    @Column(name = "divisionId")
    private String divisionId; // Changed to String to support "DIV-001" format

    @Column(name = "assignedSupervisorId", length = 255)
    private String assignedSupervisorId; // Changed to String to support both numeric IDs and email identifiers
    
    @Column(name = "assignedProfessionalId")
    private String assignedProfessionalId; // DEPRECATED: Use assignedProfessionalIds instead
    
    @Column(name = "assignedProfessionalIds", columnDefinition = "TEXT")
    private String assignedProfessionalIds; // Comma-separated list of professional IDs
    
    @Column(name = "materialCost")
    private java.math.BigDecimal materialCost;
    
    @Column(name = "laborCost")
    private java.math.BigDecimal laborCost;
    
    @Column(name = "totalCost")
    private java.math.BigDecimal totalCost;
    
    @Column(name = "partsUsed", columnDefinition = "TEXT")
    private String partsUsed;
    
    // Helper methods for multiple professional assignment
    public java.util.List<String> getAssignedProfessionalIdsList() {
        if (assignedProfessionalIds == null || assignedProfessionalIds.trim().isEmpty()) {
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
