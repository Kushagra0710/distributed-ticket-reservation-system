package com.ticketing.ticket_reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPerformanceRequest {
    @NotNull(message = "Select a screen or stage.")
    private Long venueSpaceId;

    @NotNull(message = "Performance start time is required.")
    private LocalDateTime startTime;

    @NotNull(message = "Performance end time is required.")
    private LocalDateTime endTime;

    @NotEmpty(message = "Add at least one price category.")
    @Size(max = 20, message = "A performance can have at most 20 price categories.")
    @Valid
    private List<EventPriceBandRequest> priceBands;
}
