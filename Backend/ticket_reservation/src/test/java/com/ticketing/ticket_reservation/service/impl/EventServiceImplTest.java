package com.ticketing.ticket_reservation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;

import com.ticketing.ticket_reservation.dto.EventCatalogRequest;
import com.ticketing.ticket_reservation.dto.EventPerformanceRequest;
import com.ticketing.ticket_reservation.dto.EventPriceBandRequest;
import com.ticketing.ticket_reservation.dto.ShowingsCreateRequest;
import com.ticketing.ticket_reservation.entity.Event;
import com.ticketing.ticket_reservation.entity.EventCatalog;
import com.ticketing.ticket_reservation.entity.EventCatalogPoster;
import com.ticketing.ticket_reservation.entity.Role;
import com.ticketing.ticket_reservation.entity.SeatingMode;
import com.ticketing.ticket_reservation.entity.User;
import com.ticketing.ticket_reservation.entity.Venue;
import com.ticketing.ticket_reservation.entity.VenueSpace;
import com.ticketing.ticket_reservation.repository.EventCatalogRepository;
import com.ticketing.ticket_reservation.repository.EventCatalogPosterRepository;
import com.ticketing.ticket_reservation.repository.EventPriceBandRepository;
import com.ticketing.ticket_reservation.repository.EventRepository;
import com.ticketing.ticket_reservation.repository.VenueRepository;
import com.ticketing.ticket_reservation.repository.VenueSpaceRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {
    @Mock private EventCatalogRepository catalogRepository;
    @Mock private EventCatalogPosterRepository posterRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventPriceBandRepository priceBandRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private VenueSpaceRepository venueSpaceRepository;
    @InjectMocks private EventServiceImpl eventService;

    private Venue venue;
    private VenueSpace space;
    private EventCatalog catalogEvent;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        venue = new Venue();
        venue.setId(2L);
        venue.setName("Grand Theatre");
        venue.setVenueType("Theatre");
        venue.setDeleteFlag(false);
        space = new VenueSpace();
        space.setId(3L);
        space.setName("Screen 1");
        space.setVenue(venue);
        space.setSeatingMode(SeatingMode.RESERVED);
        space.setCapacity(100);
        space.setRowCount(10);
        space.setSeatsPerRow(10);
        catalogEvent = new EventCatalog();
        catalogEvent.setId(9L);
        catalogEvent.setTitle("Drishyam");
        catalogEvent.setPosterUrl("https://cdn.example.test/drishyam.jpg");
        catalogEvent.setActive(true);

        lenient().when(venueRepository.findById(2L)).thenReturn(Optional.of(venue));
        lenient().when(venueSpaceRepository.findByIdAndVenue_IdAndDeleteFlagFalse(3L, 2L))
            .thenReturn(Optional.of(space));
        lenient().when(catalogRepository.findById(9L)).thenReturn(Optional.of(catalogEvent));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void stubAvailableSchedule() {
        when(eventRepository.existsByVenueSpace_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Mockito.eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event showing = invocation.getArgument(0);
            showing.setId(12L);
            return showing;
        });
        when(priceBandRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsUniqueCatalogEntryWithSharedPosterMetadata() {
        EventCatalogRequest request = new EventCatalogRequest();
        request.setTitle(" Drishyam ");
        when(catalogRepository.findByNormalizedTitle("drishyam")).thenReturn(Optional.empty());
        when(catalogRepository.save(any(EventCatalog.class))).thenAnswer(invocation -> {
            EventCatalog saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        var result = eventService.createEvent(request, null);

        assertEquals("Drishyam", result.title());
        assertNull(result.posterUrl());
        verify(catalogRepository).save(any(EventCatalog.class));
    }

    @Test
    void uploadsPosterImageAndReturnsPublicImagePath() {
        EventCatalogRequest request = new EventCatalogRequest();
        request.setTitle(" Drishyam ");
        when(catalogRepository.findByNormalizedTitle("drishyam")).thenReturn(Optional.empty());
        when(catalogRepository.save(any(EventCatalog.class))).thenAnswer(invocation -> {
            EventCatalog saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        MockMultipartFile poster = new MockMultipartFile(
            "poster",
            "poster.png",
            "image/png",
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/pWQAAAAASUVORK5CYII="
            )
        );

        var result = eventService.createEvent(request, poster);

        assertEquals("/events/9/poster", result.posterUrl());
        ArgumentCaptor<EventCatalog> eventCaptor = ArgumentCaptor.forClass(EventCatalog.class);
        verify(catalogRepository).save(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().isPosterUploaded());
        ArgumentCaptor<EventCatalogPoster> posterCaptor = ArgumentCaptor.forClass(EventCatalogPoster.class);
        verify(posterRepository).save(posterCaptor.capture());
        assertEquals(9L, posterCaptor.getValue().getEventId());
        assertEquals("image/png", posterCaptor.getValue().getContentType());
        assertTrue(posterCaptor.getValue().getImageData().length > 0);
    }

    @Test
    void refusesToDeleteCatalogEventLinkedToShowings() {
        when(catalogRepository.findById(9L)).thenReturn(Optional.of(catalogEvent));
        when(eventRepository.existsByCatalogEvent_Id(9L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> eventService.deleteEvent(9L));
        verify(catalogRepository, never()).delete(any(EventCatalog.class));
    }

    @Test
    void deletesCatalogEventWhenNoShowingReferencesIt() {
        when(catalogRepository.findById(9L)).thenReturn(Optional.of(catalogEvent));
        when(eventRepository.existsByCatalogEvent_Id(9L)).thenReturn(false);

        eventService.deleteEvent(9L);

        verify(catalogRepository).delete(catalogEvent);
    }

    @Test
    void rejectsDuplicateCatalogTitlesCaseInsensitively() {
        EventCatalogRequest request = new EventCatalogRequest();
        request.setTitle("DRISHYAM");
        when(catalogRepository.findByNormalizedTitle("drishyam")).thenReturn(Optional.of(catalogEvent));

        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(request, null));
        verify(catalogRepository, never()).save(any(EventCatalog.class));
    }

    @Test
    void createsShowingLinkedToCatalogEventWithItsOwnPricing() {
        ShowingsCreateRequest request = request(
            performance("2026-10-12T18:00", "2026-10-12T20:00",
                band("Standard", 1, 8, "250.00"),
                band("Premium", 9, 10, "400.00"))
        );
        stubAvailableSchedule();

        var created = eventService.createShowings(request);

        assertEquals(1, created.size());
        assertEquals(9L, created.get(0).eventId());
        assertEquals("Drishyam", created.get(0).title());
        assertEquals("Screen 1", created.get(0).venueSpaceName());
        assertEquals(2, created.get(0).priceBands().size());
        assertEquals("Premium", created.get(0).priceBands().get(1).categoryName());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createsMultipleScreenAndTimeCombinationsForOneCatalogEvent() {
        EventPerformanceRequest first = performance("2026-10-12T18:00", "2026-10-12T20:00",
            band("Standard", 1, 10, "250.00"));
        EventPerformanceRequest second = performance("2026-10-12T20:30", "2026-10-12T22:30",
            band("Standard", 1, 10, "300.00"));
        stubAvailableSchedule();

        var created = eventService.createShowings(request(first, second));

        assertEquals(2, created.size());
        verify(eventRepository, Mockito.times(2)).save(any(Event.class));
    }

    @Test
    void createsGeneralAdmissionShowingWithSinglePrice() {
        space.setSeatingMode(SeatingMode.GENERAL_ADMISSION);
        space.setRowCount(null);
        ShowingsCreateRequest request = request(performance("2026-10-12T18:00", "2026-10-12T20:00",
            band("General admission", null, null, "150.00")));
        stubAvailableSchedule();

        var created = eventService.createShowings(request);

        assertEquals(1, created.size());
        assertEquals("General admission", created.get(0).priceBands().get(0).categoryName());
        assertNull(created.get(0).priceBands().get(0).rowFrom());
    }

    @Test
    void rejectsOverlappingRowsBetweenPriceCategories() {
        ShowingsCreateRequest request = request(performance("2026-10-12T18:00", "2026-10-12T20:00",
            band("Standard", 1, 6, "250.00"),
            band("Premium", 6, 10, "400.00")));

        assertThrows(IllegalArgumentException.class, () -> eventService.createShowings(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void rejectsOverlappingPerformancesForSameScreen() {
        EventPerformanceRequest first = performance("2026-10-12T18:00", "2026-10-12T20:00",
            band("Standard", 1, 10, "250.00"));
        EventPerformanceRequest second = performance("2026-10-12T19:30", "2026-10-12T21:00",
            band("Standard", 1, 10, "300.00"));
        when(eventRepository.existsByVenueSpace_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Mockito.eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> eventService.createShowings(request(first, second)));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void ownerCannotCreateShowingAtAnotherOwnersVenue() {
        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setRole(Role.OWNER);
        venue.setOwner(owner);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "other-owner@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        ));

        assertThrows(AccessDeniedException.class,
            () -> eventService.createShowings(request(performance("2026-10-12T18:00", "2026-10-12T20:00",
                band("Standard", 1, 10, "250.00")))));
        verify(catalogRepository, never()).findById(9L);
    }

    @Test
    void cityDiscoveryReturnsCatalogEventsOnly() {
        when(catalogRepository.findDiscoverableEventsByCity(
            Mockito.eq("Pune"), any(LocalDateTime.class)))
            .thenReturn(List.of(catalogEvent));

        var events = eventService.getDiscoverableEvents(" Pune ");

        assertEquals(1, events.size());
        assertEquals("Drishyam", events.get(0).title());
        assertEquals("https://cdn.example.test/drishyam.jpg", events.get(0).posterUrl());
        verify(eventRepository, never()).findUpcomingShowingsForCatalogEvent(
            Mockito.anyLong(), Mockito.anyString(), any(LocalDateTime.class));
    }

    @Test
    void eventDetailsFetchShowingsForTheSelectedCity() {
        Venue secondVenue = new Venue();
        secondVenue.setId(4L);
        secondVenue.setName("Riverside Cinema");
        VenueSpace secondSpace = new VenueSpace();
        secondSpace.setId(6L);
        secondSpace.setName("Screen 2");
        Event firstShowing = showing(21L, venue, space);
        Event secondShowing = showing(22L, secondVenue, secondSpace);
        when(eventRepository
            .findUpcomingShowingsForCatalogEvent(
                Mockito.eq(9L), Mockito.eq("Pune"), any(LocalDateTime.class)))
            .thenReturn(List.of(firstShowing, secondShowing));
        when(priceBandRepository.findAllByEvent_IdInOrderByIdAsc(anyList())).thenReturn(List.of());

        var showings = eventService.getDiscoverableShowings(9L, " Pune ");

        assertEquals(2, showings.size());
        assertEquals("Grand Theatre", showings.get(0).venueName());
        assertEquals("Riverside Cinema", showings.get(1).venueName());
    }

    private Event showing(Long id, Venue showingVenue, VenueSpace showingSpace) {
        Event showing = new Event();
        showing.setId(id);
        showing.setTitle(catalogEvent.getTitle());
        showing.setCatalogEvent(catalogEvent);
        showing.setVenue(showingVenue);
        showing.setVenueSpace(showingSpace);
        showing.setStartTime(LocalDateTime.parse("2026-10-12T18:00"));
        showing.setEndTime(LocalDateTime.parse("2026-10-12T20:00"));
        return showing;
    }

    private ShowingsCreateRequest request(EventPerformanceRequest... performances) {
        ShowingsCreateRequest request = new ShowingsCreateRequest();
        request.setEventId(9L);
        request.setVenueId(2L);
        request.setPerformances(List.of(performances));
        return request;
    }

    private EventPerformanceRequest performance(
            String start,
            String end,
            EventPriceBandRequest... priceBands) {
        EventPerformanceRequest request = new EventPerformanceRequest();
        request.setVenueSpaceId(3L);
        request.setStartTime(LocalDateTime.parse(start));
        request.setEndTime(LocalDateTime.parse(end));
        request.setPriceBands(List.of(priceBands));
        return request;
    }

    private EventPriceBandRequest band(String name, Integer rowFrom, Integer rowTo, String price) {
        EventPriceBandRequest band = new EventPriceBandRequest();
        band.setCategoryName(name);
        band.setRowFrom(rowFrom);
        band.setRowTo(rowTo);
        band.setPrice(new BigDecimal(price));
        return band;
    }
}
