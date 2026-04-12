package com.org.cmbms.maintenance.model;

import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
@Getter
@Setter
public class StatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "requestId")
    private Long requestId;
    @Enumerated(EnumType.STRING)
    @Column(name = "requestType")
    private RequestType requestType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    @Column(name = "changedBy")
    private Long changedBy;
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}

