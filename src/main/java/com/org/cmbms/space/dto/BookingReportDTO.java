package com.org.cmbms.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class BookingReportDTO {
    private Long id;
    private Long assignmentId;
    private String reportText;
    private LocalDateTime createdAt;
    private String createdBy;
    private boolean viewed;
}
