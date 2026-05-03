package com.org.cmbms.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BookingRequestDTO {
    @NotBlank
    private String bookingId;
    @NotBlank
    private String type;
    @NotNull
    private String requester; // Changed to String to support both numeric IDs and email identifiers
    @NotNull
    private LocalDateTime dateTime;
    private LocalDateTime endTime;
    @NotNull
    private Integer capacity;
    @NotBlank
    private String layout;
    @NotBlank
    private String amenities;
    private String divisionId; // Changed to String to support "DIV-001" format
}

