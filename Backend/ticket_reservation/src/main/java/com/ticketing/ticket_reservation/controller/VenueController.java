package com.ticketing.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.constants.ApplicationConstants;
import com.ticketing.ticket_reservation.dto.AdminUserResponse;
import com.ticketing.ticket_reservation.dto.PincodeLocation;
import com.ticketing.ticket_reservation.dto.VenueRequest;
import com.ticketing.ticket_reservation.dto.VenueSpaceRequest;
import com.ticketing.ticket_reservation.dto.VenueSpaceResponse;
import com.ticketing.ticket_reservation.dto.VenueSpacesResponse;
import com.ticketing.ticket_reservation.entity.Venue;
import com.ticketing.ticket_reservation.service.interfaces.VenueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping (ApplicationConstants.ADMIN_URL)
@RequiredArgsConstructor 
public class VenueController {

    private final VenueService venueService;

    @GetMapping(ApplicationConstants.VENUE)
    public ResponseEntity<?> getVenues(){
        List<Venue> response = venueService.getAllVenue();
        return ResponseEntity.ok(response);
    }

    @GetMapping(ApplicationConstants.VENUE + "/pincode/{pincode}")
    public ResponseEntity<PincodeLocation> getLocationByPincode(@PathVariable String pincode) {
        return ResponseEntity.ok(venueService.getLocationByPincode(pincode));
    }
    
    @PostMapping(ApplicationConstants.VENUE)
    public ResponseEntity<AdminUserResponse> addVenue(@Valid @RequestBody VenueRequest request){
        AdminUserResponse response = venueService.addVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping (ApplicationConstants.VENUE + "/{id}")
    public ResponseEntity<AdminUserResponse> editVenue(@Valid @RequestBody VenueRequest request, @PathVariable Long id ){
        AdminUserResponse response = venueService.editVenue(request,id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping (ApplicationConstants.VENUE + "/{id}" + ApplicationConstants.DELETE)
    public ResponseEntity<?> deleteVenue(@Valid @PathVariable Long id){
        AdminUserResponse response = venueService.deleteVenue(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(ApplicationConstants.VENUE + "/{venueId}/spaces")
    public ResponseEntity<VenueSpacesResponse> getVenueSpaces(@PathVariable Long venueId) {
        return ResponseEntity.ok(venueService.getVenueSpaces(venueId));
    }

    @PostMapping(ApplicationConstants.VENUE + "/{venueId}/spaces")
    public ResponseEntity<VenueSpaceResponse> addVenueSpace(
            @PathVariable Long venueId,
            @Valid @RequestBody VenueSpaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(venueService.addVenueSpace(venueId, request));
    }

    @PutMapping(ApplicationConstants.VENUE + "/{venueId}/spaces/{spaceId}")
    public ResponseEntity<VenueSpaceResponse> editVenueSpace(
            @PathVariable Long venueId,
            @PathVariable Long spaceId,
            @Valid @RequestBody VenueSpaceRequest request) {
        return ResponseEntity.ok(venueService.editVenueSpace(venueId, spaceId, request));
    }

    @DeleteMapping(ApplicationConstants.VENUE + "/{venueId}/spaces/{spaceId}")
    public ResponseEntity<AdminUserResponse> deleteVenueSpace(
            @PathVariable Long venueId,
            @PathVariable Long spaceId) {
        return ResponseEntity.ok(venueService.deleteVenueSpace(venueId, spaceId));
    }
    
}
