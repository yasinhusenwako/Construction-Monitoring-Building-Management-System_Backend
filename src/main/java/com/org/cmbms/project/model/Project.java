
package com.org.cmbms.project.model;

import com.org.cmbms.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "projectId")
    private String projectId;
    @Column(name = "title")
    private String title;
    @Column(name = "location")
    private String location;
    @Column(name = "block")
    private String block;
    @Column(name = "floor")
    private String floor;
    @Column(name = "department")
    private String department;
    @Column(name = "contactPerson")
    private String contactPerson;
    @Column(name = "phone")
    private String phone;
    @Column(name = "siteCondition")
    private String siteCondition;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "budget")
    private BigDecimal budget;
    @Column(name = "startDate")
    private LocalDate startDate;
    @Column(name = "endDate")
    private LocalDate endDate;
    @Column(name = "classification")
    private String classification;
    @Column(name = "priority")
    private String priority;
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
    
    @Column(name="assignedProfessionalId")
    private String assignedProfessionalId; // Changed to String to support both numeric IDs and email identifiers

    @Column(name = "boqApproved")
    private Boolean boqApproved = Boolean.FALSE;
    
    @Column(name = "materialCost")
    private BigDecimal materialCost;
    
    @Column(name = "laborCost")
    private BigDecimal laborCost;
    
    @Column(name = "totalCost")
    private BigDecimal totalCost;
    
    @Column(name = "partsUsed", columnDefinition = "TEXT")
    private String partsUsed;

    @Column(name = "requestMode")
    private String requestMode;

    @Column(name = "linkedProjectId")
    private String linkedProjectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope", columnDefinition = "jsonb")
    private Map<String, Object> scope;
}
