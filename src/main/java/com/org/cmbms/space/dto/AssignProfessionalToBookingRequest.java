package com.org.cmbms.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignProfessionalToBookingRequest {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
    
    @NotBlank(message = "Professional ID is required")
    private String professionalId;
    
    @NotBlank(message = "Instructions are required")
    private String instructions;
}
