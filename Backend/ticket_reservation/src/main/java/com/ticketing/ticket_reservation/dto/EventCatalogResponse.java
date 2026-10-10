package com.ticketing.ticket_reservation.dto;

public record EventCatalogResponse(
    Long id,
    String title,
    String description,
    String posterUrl,
    Integer durationMinutes
) {}
