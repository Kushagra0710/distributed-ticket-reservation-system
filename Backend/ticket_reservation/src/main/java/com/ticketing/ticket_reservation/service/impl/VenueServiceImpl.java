package com.ticketing.ticket_reservation.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ticketing.ticket_reservation.dto.AdminUserResponse;
import com.ticketing.ticket_reservation.dto.PincodeLocation;
import com.ticketing.ticket_reservation.dto.VenueRequest;
import com.ticketing.ticket_reservation.dto.VenueSpaceRequest;
import com.ticketing.ticket_reservation.dto.VenueSpaceResponse;
import com.ticketing.ticket_reservation.dto.VenueSpacesResponse;
import com.ticketing.ticket_reservation.entity.SeatingMode;
import com.ticketing.ticket_reservation.entity.SpaceType;
import com.ticketing.ticket_reservation.entity.Venue;
import com.ticketing.ticket_reservation.entity.VenueSpace;
import com.ticketing.ticket_reservation.repository.VenueRepository;
import com.ticketing.ticket_reservation.repository.VenueSpaceRepository;
import com.ticketing.ticket_reservation.repository.UserRepository;
import com.ticketing.ticket_reservation.service.interfaces.VenueService;
import com.ticketing.ticket_reservation.utility.PostalPincodeUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {
    private final VenueRepository venueRepo;
    private final VenueSpaceRepository venueSpaceRepo;
    private final UserRepository userRepository;
    private final PostalPincodeUtil postalPincodeUtil;

    @Override
    public List<Venue> getAllVenue() {
        if (!isAdmin()) {
            return venueRepo.findAllByDeleteFlagFalseAndOwner_EmailOrderByNameAsc(currentEmail());
        }
        return venueRepo.findAllByDeleteFlagFalse();
    }

    @Override
    public PincodeLocation getLocationByPincode(String pincode) {
        return postalPincodeUtil.getLocationByPincode(pincode);
    }

    @Override
    public AdminUserResponse addVenue(VenueRequest request) {
        Venue venue = new Venue();
        applyRequest(venue, request);
        assignOwner(venue, request.getOwnerId());
        venueRepo.save(venue);
        return new AdminUserResponse(true, "Venue created successfully.");
    }

    @Override
    public AdminUserResponse editVenue(VenueRequest request, Long id) {
        Venue venue = venueRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Venue not found."));
        applyRequest(venue, request);
        assignOwner(venue, request.getOwnerId());
        venueRepo.save(venue);
        return new AdminUserResponse(true, "Venue updated successfully.");
    }

    @Override 
    public AdminUserResponse deleteVenue(Long id){
        Venue venue = venueRepo.findById(id)
                    .orElseThrow(()-> new IllegalArgumentException("Venue no found."));
        
        if(venue.getDeleteFlag()==true)
            throw new IllegalArgumentException("Venue is already deleted.");

        venue.setDeleteFlag(true);
        venueRepo.save(venue);
        return new AdminUserResponse(true, "Venue deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSpacesResponse getVenueSpaces(Long venueId) {
        Venue venue = findActiveVenue(venueId);
        SpaceType spaceType = getSpaceType(venue);
        List<VenueSpaceResponse> spaces = venueSpaceRepo
            .findAllByVenue_IdAndDeleteFlagFalseOrderByIdAsc(venueId)
            .stream()
            .map(this::toSpaceResponse)
            .toList();
        return new VenueSpacesResponse(venue.getId(), venue.getName(), spaceType, spaces);
    }

    @Override
    @Transactional
    public VenueSpaceResponse addVenueSpace(Long venueId, VenueSpaceRequest request) {
        Venue venue = findActiveVenue(venueId);
        SpaceType spaceType = getSpaceType(venue);
        validateSpaceName(venueId, request.getName(), null);

        VenueSpace space = new VenueSpace();
        space.setVenue(venue);
        applySpaceRequest(space, spaceType, request);
        return toSpaceResponse(venueSpaceRepo.save(space));
    }

    @Override
    @Transactional
    public VenueSpaceResponse editVenueSpace(Long venueId, Long spaceId, VenueSpaceRequest request) {
        Venue venue = findActiveVenue(venueId);
        SpaceType spaceType = getSpaceType(venue);
        VenueSpace space = venueSpaceRepo.findByIdAndVenue_IdAndDeleteFlagFalse(spaceId, venueId)
            .orElseThrow(() -> new IllegalArgumentException("Screen or stage not found."));
        validateSpaceName(venueId, request.getName(), spaceId);

        applySpaceRequest(space, spaceType, request);
        return toSpaceResponse(venueSpaceRepo.save(space));
    }

    @Override
    @Transactional
    public AdminUserResponse deleteVenueSpace(Long venueId, Long spaceId) {
        Venue venue = findActiveVenue(venueId);
        VenueSpace space = venueSpaceRepo.findByIdAndVenue_IdAndDeleteFlagFalse(spaceId, venue.getId())
            .orElseThrow(() -> new IllegalArgumentException("Screen or stage not found."));
        space.setDeleteFlag(true);
        venueSpaceRepo.save(space);
        return new AdminUserResponse(true, "Screen or stage removed successfully.");
    }

    private Venue findActiveVenue(Long venueId) {
        Venue venue = venueRepo.findById(venueId)
            .orElseThrow(() -> new IllegalArgumentException("Venue not found."));
        if (Boolean.TRUE.equals(venue.getDeleteFlag())) {
            throw new IllegalArgumentException("Venue is deleted.");
        }
        ensureVenueAccess(venue);
        return venue;
    }

    private SpaceType getSpaceType(Venue venue) {
        return switch (venue.getVenueType()) {
            case "Theatre" -> SpaceType.SCREEN;
            case "Auditorium" -> SpaceType.HALL;
            case "Concert Ground" -> SpaceType.STAGE;
            default -> throw new IllegalArgumentException("Unsupported venue type.");
        };
    }

    private void validateSpaceName(Long venueId, String name, Long currentSpaceId) {
        boolean alreadyExists = currentSpaceId == null
            ? venueSpaceRepo.existsByVenue_IdAndNameIgnoreCase(venueId, name.trim())
            : venueSpaceRepo.existsByVenue_IdAndNameIgnoreCaseAndIdNot(venueId, name.trim(), currentSpaceId);
        if (alreadyExists) {
            throw new IllegalArgumentException("A screen or stage with this name already exists in the venue.");
        }
    }

    private void applySpaceRequest(VenueSpace space, SpaceType spaceType, VenueSpaceRequest request) {
        space.setName(request.getName().trim());
        space.setSpaceType(spaceType);
        space.setSeatingMode(request.getSeatingMode());

        if (request.getSeatingMode() == SeatingMode.RESERVED) {
            Integer rows = request.getRows();
            Integer seatsPerRow = request.getSeatsPerRow();
            if (rows == null || seatsPerRow == null || request.getCapacity() != null) {
                throw new IllegalArgumentException("For reserved seating, provide rows and seats per row only.");
            }
            int capacity = rows * seatsPerRow;
            if (capacity > 10000) {
                throw new IllegalArgumentException("A screen or stage cannot exceed 10000 seats.");
            }
            space.setCapacity(capacity);
            space.setRowCount(rows);
            space.setSeatsPerRow(seatsPerRow);
            return;
        }

        Integer capacity = request.getCapacity();
        if (capacity == null || request.getRows() != null || request.getSeatsPerRow() != null) {
            throw new IllegalArgumentException("For general admission, provide a capacity only.");
        }
        space.setCapacity(capacity);
        space.setRowCount(null);
        space.setSeatsPerRow(null);
    }

    private VenueSpaceResponse toSpaceResponse(VenueSpace space) {
        return new VenueSpaceResponse(
            space.getId(),
            space.getName(),
            space.getSpaceType(),
            space.getSeatingMode(),
            space.getCapacity(),
            space.getRowCount(),
            space.getSeatsPerRow()
        );
    }

    private void applyRequest(Venue venue, VenueRequest request) {
        PincodeLocation location =
            postalPincodeUtil.getLocationByPincode(request.getPincode().toString());

        venue.setName(request.getName().trim());
        venue.setAddress(request.getAddress().trim());
        venue.setPincode(request.getPincode());
        venue.setCity(location.city());
        venue.setState(location.state());
        venue.setVenueType(request.getVenueType());
    }

    private void assignOwner(Venue venue, Long ownerId) {
        if (ownerId == null) {
            venue.setOwner(null);
            return;
        }
        var owner = userRepository.findById(ownerId)
            .filter(user -> user.getRole() == com.ticketing.ticket_reservation.entity.Role.OWNER)
            .filter(user -> user.isActive())
            .orElseThrow(() -> new IllegalArgumentException("Select an active owner account."));
        venue.setOwner(owner);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }

    private void ensureVenueAccess(Venue venue) {
        if (!isAdmin() && (venue.getOwner() == null
                || !currentEmail().equalsIgnoreCase(venue.getOwner().getEmail()))) {
            throw new AccessDeniedException("You do not have access to this venue.");
        }
    }
}
