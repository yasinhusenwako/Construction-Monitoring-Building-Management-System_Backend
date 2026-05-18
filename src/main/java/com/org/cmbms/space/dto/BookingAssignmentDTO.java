package com.org.cmbms.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class BookingAssignmentDTO {
    private Long id;
    private Long bookingId;
    private String professionalId;
    private String instructions;
    private LocalDateTime createdAt;
    private String createdBy;
    private String status;
}
