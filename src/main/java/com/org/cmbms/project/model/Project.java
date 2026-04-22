
package com.org.cmbms.project.model;

import com.org.cmbms.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Column(name = "createdBy")
    private Long createdBy;
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    @Column(name = "divisionId")
    private Long divisionId;
    @Column(name = "assignedSupervisorId")
    private Long assignedSupervisorId;
    @Column(name="assignedProfessionalId")
    private Long assignedProfessionalId;

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
}
