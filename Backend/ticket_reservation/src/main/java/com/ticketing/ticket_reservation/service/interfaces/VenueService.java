package com.ticketing.ticket_reservation.service.interfaces;

import java.util.List;

import com.ticketing.ticket_reservation.dto.AdminUserResponse;
import com.ticketing.ticket_reservation.dto.PincodeLocation;
import com.ticketing.ticket_reservation.dto.VenueRequest;
import com.ticketing.ticket_reservation.dto.VenueSpaceRequest;
import com.ticketing.ticket_reservation.dto.VenueSpaceResponse;
import com.ticketing.ticket_reservation.dto.VenueSpacesResponse;
import com.ticketing.ticket_reservation.entity.Venue;

public interface VenueService {
    List<Venue> getAllVenue();
    PincodeLocation getLocationByPincode(String pincode);
    AdminUserResponse addVenue(VenueRequest request);
    AdminUserResponse editVenue(VenueRequest request, Long id);
    AdminUserResponse deleteVenue(Long id);
    VenueSpacesResponse getVenueSpaces(Long venueId);
    VenueSpaceResponse addVenueSpace(Long venueId, VenueSpaceRequest request);
    VenueSpaceResponse editVenueSpace(Long venueId, Long spaceId, VenueSpaceRequest request);
    AdminUserResponse deleteVenueSpace(Long venueId, Long spaceId);
}
