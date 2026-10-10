package com.ticketing.ticket_reservation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ticketing.ticket_reservation.dto.VenueSpaceRequest;
import com.ticketing.ticket_reservation.entity.SeatingMode;
import com.ticketing.ticket_reservation.entity.Role;
import com.ticketing.ticket_reservation.entity.User;
import com.ticketing.ticket_reservation.entity.Venue;
import com.ticketing.ticket_reservation.entity.VenueSpace;
import com.ticketing.ticket_reservation.repository.VenueRepository;
import com.ticketing.ticket_reservation.repository.VenueSpaceRepository;
import com.ticketing.ticket_reservation.repository.UserRepository;
import com.ticketing.ticket_reservation.utility.PostalPincodeUtil;

@ExtendWith(MockitoExtension.class)
class VenueSpaceServiceImplTest {
    @Mock
    private VenueRepository venueRepo;

    @Mock
    private VenueSpaceRepository venueSpaceRepo;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostalPincodeUtil postalPincodeUtil;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue theatre;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        theatre = new Venue();
        theatre.setId(5L);
        theatre.setName("Central Theatre");
        theatre.setVenueType("Theatre");
        theatre.setDeleteFlag(false);
        when(venueRepo.findById(5L)).thenReturn(Optional.of(theatre));
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsReservedScreenWithCalculatedCapacity() {
        VenueSpaceRequest request = new VenueSpaceRequest();
        request.setName("Screen 1");
        request.setSeatingMode(SeatingMode.RESERVED);
        request.setRows(3);
        request.setSeatsPerRow(8);
        when(venueSpaceRepo.existsByVenue_IdAndNameIgnoreCase(5L, "Screen 1")).thenReturn(false);
        when(venueSpaceRepo.save(any(VenueSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = venueService.addVenueSpace(5L, request);

        assertEquals(24, response.capacity());
        assertEquals(3, response.rows());
        assertEquals(8, response.seatsPerRow());
    }

    @Test
    void createsGeneralAdmissionStageWithoutSeatRecords() {
        theatre.setVenueType("Concert Ground");
        VenueSpaceRequest request = new VenueSpaceRequest();
        request.setName("Main Stage");
        request.setSeatingMode(SeatingMode.GENERAL_ADMISSION);
        request.setCapacity(500);
        when(venueSpaceRepo.existsByVenue_IdAndNameIgnoreCase(5L, "Main Stage")).thenReturn(false);
        when(venueSpaceRepo.save(any(VenueSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = venueService.addVenueSpace(5L, request);

        assertEquals(500, response.capacity());
        assertEquals(SeatingMode.GENERAL_ADMISSION, response.seatingMode());
    }

    @Test
    void rejectsReservedLayoutOverCapacityLimit() {
        VenueSpaceRequest request = new VenueSpaceRequest();
        request.setName("Oversized Screen");
        request.setSeatingMode(SeatingMode.RESERVED);
        request.setRows(101);
        request.setSeatsPerRow(100);
        when(venueSpaceRepo.existsByVenue_IdAndNameIgnoreCase(5L, "Oversized Screen")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> venueService.addVenueSpace(5L, request));
    }

    @Test
    void ownerCannotReadAnotherOwnersVenueSpaces() {
        User owner = new User();
        owner.setEmail("other-owner@example.com");
        owner.setRole(Role.OWNER);
        theatre.setOwner(owner);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "owner@example.com",
            null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        ));

        assertThrows(AccessDeniedException.class, () -> venueService.getVenueSpaces(5L));
    }
}
