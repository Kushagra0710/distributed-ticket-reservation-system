package com.ticketing.ticket_reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ShowingResponse(
    Long id,
    Long eventId,
    String title,
    String posterUrl,
    Long venueId,
    String venueName,
    Long venueSpaceId,
    String venueSpaceName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<EventPriceBandResponse> priceBands
) {}
