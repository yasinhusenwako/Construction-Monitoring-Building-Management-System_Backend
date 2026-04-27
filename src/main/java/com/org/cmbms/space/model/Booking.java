
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
    private Long requester;
    @Column(name = "dateTime")
    private LocalDateTime dateTime;
    @Column(name = "capacity")
    private Integer capacity;
    @Column(name = "layout")
    private String layout;
    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;
    @Column(name = "divisionId")
    private Long divisionId;

    @Column(name = "assignedSupervisorId")
    private Long assignedSupervisorId;
    @Column(name = "assignedProfessionalId")
    private Long assignedProfessionalId;
    
    @Column(name = "materialCost")
    private java.math.BigDecimal materialCost;
    
    @Column(name = "laborCost")
    private java.math.BigDecimal laborCost;
    
    @Column(name = "totalCost")
    private java.math.BigDecimal totalCost;
    
    @Column(name = "partsUsed", columnDefinition = "TEXT")
    private String partsUsed;
}
