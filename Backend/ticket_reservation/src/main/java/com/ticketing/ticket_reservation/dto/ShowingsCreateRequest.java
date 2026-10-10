package com.ticketing.ticket_reservation.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowingsCreateRequest {
    @NotNull(message = "Select an event.")
    private Long eventId;

    @NotNull(message = "Select a venue.")
    private Long venueId;

    @NotEmpty(message = "Add at least one screen/time combination.")
    @Size(max = 50, message = "A request can have at most 50 screen/time combinations.")
    @Valid
    private List<EventPerformanceRequest> performances;
}
