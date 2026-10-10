package com.ticketing.ticket_reservation.dto;

import com.ticketing.ticket_reservation.entity.SeatingMode;
import com.ticketing.ticket_reservation.entity.SpaceType;

public record VenueSpaceResponse(
    Long id,
    String name,
    SpaceType spaceType,
    SeatingMode seatingMode,
    int capacity,
    Integer rows,
    Integer seatsPerRow
) {}
