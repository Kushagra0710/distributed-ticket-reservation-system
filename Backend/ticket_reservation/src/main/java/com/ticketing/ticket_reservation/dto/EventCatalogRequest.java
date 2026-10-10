package com.ticketing.ticket_reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventCatalogRequest {
    @NotBlank(message = "Event name is required.")
    @Size(max = 120, message = "Event name must be 120 characters or fewer.")
    private String title;

    @Size(max = 5000, message = "Description must be 5000 characters or fewer.")
    private String description;

    @Min(value = 1, message = "Duration must be greater than zero.")
    @Max(value = 1440, message = "Duration must be one day or less.")
    private Integer durationMinutes;
}
