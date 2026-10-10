package com.ticketing.ticket_reservation.dto;

import java.util.List;

public record EventDiscoveryResponse(
    Long id,
    String title,
    String description,
    String posterUrl,
    Integer durationMinutes,
    List<ShowingResponse> showings
) {}
