package com.ticketing.ticket_reservation.dto;

import com.ticketing.ticket_reservation.entity.SeatingMode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueSpaceRequest {
    @NotBlank(message = "Screen or stage name is required.")
    @Size(max = 80, message = "Name must be 80 characters or fewer.")
    private String name;

    @NotNull(message = "Seating mode is required.")
    private SeatingMode seatingMode;

    @Min(value = 1, message = "Capacity must be at least 1.")
    @Max(value = 10000, message = "Capacity cannot exceed 10000.")
    private Integer capacity;

    @Min(value = 1, message = "Rows must be at least 1.")
    @Max(value = 200, message = "Rows cannot exceed 200.")
    private Integer rows;

    @Min(value = 1, message = "Seats per row must be at least 1.")
    @Max(value = 200, message = "Seats per row cannot exceed 200.")
    private Integer seatsPerRow;
}
