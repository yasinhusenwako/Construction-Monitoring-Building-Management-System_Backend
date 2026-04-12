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
    private Long requester;
    @NotNull
    private LocalDateTime dateTime;
    @NotNull
    private Integer capacity;
    @NotBlank
    private String layout;
    @NotBlank
    private String amenities;
    private Long divisionId;
}

