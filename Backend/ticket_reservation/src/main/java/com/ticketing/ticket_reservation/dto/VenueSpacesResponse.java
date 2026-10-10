package com.ticketing.ticket_reservation.dto;

import java.util.List;

import com.ticketing.ticket_reservation.entity.SpaceType;

public record VenueSpacesResponse(
    Long venueId,
    String venueName,
    SpaceType spaceType,
    List<VenueSpaceResponse> spaces
) {
}
