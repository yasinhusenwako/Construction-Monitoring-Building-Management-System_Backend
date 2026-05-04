
package com.org.cmbms.maintenance.model;

import com.org.cmbms.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "maintenanceRequestId")
    private Long maintenanceRequestId;
    @Column(name = "assignedProfessionalId")
    private String assignedProfessionalId; // Changed to String to support both numeric IDs and email identifiers
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
}
