package com.ticketing.ticket_reservation.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ticketing.ticket_reservation.dto.EventCatalogRequest;
import com.ticketing.ticket_reservation.dto.EventCatalogResponse;
import com.ticketing.ticket_reservation.dto.EventDiscoveryResponse;
import com.ticketing.ticket_reservation.dto.EventPoster;
import com.ticketing.ticket_reservation.dto.EventPerformanceRequest;
import com.ticketing.ticket_reservation.dto.EventPriceBandRequest;
import com.ticketing.ticket_reservation.dto.EventPriceBandResponse;
import com.ticketing.ticket_reservation.dto.ShowingResponse;
import com.ticketing.ticket_reservation.dto.ShowingsCreateRequest;
import com.ticketing.ticket_reservation.entity.Event;
import com.ticketing.ticket_reservation.entity.EventCatalog;
import com.ticketing.ticket_reservation.entity.EventCatalogPoster;
import com.ticketing.ticket_reservation.entity.EventPriceBand;
import com.ticketing.ticket_reservation.entity.SeatingMode;
import com.ticketing.ticket_reservation.entity.Venue;
import com.ticketing.ticket_reservation.entity.VenueSpace;
import com.ticketing.ticket_reservation.repository.EventCatalogRepository;
import com.ticketing.ticket_reservation.repository.EventCatalogPosterRepository;
import com.ticketing.ticket_reservation.repository.EventPriceBandRepository;
import com.ticketing.ticket_reservation.repository.EventRepository;
import com.ticketing.ticket_reservation.repository.VenueRepository;
import com.ticketing.ticket_reservation.repository.VenueSpaceRepository;
import com.ticketing.ticket_reservation.service.interfaces.EventService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private static final long MAX_POSTER_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_POSTER_DIMENSION = 8000;
    private static final long MAX_POSTER_PIXELS = 40_000_000L;
    private final EventCatalogRepository catalogRepository;
    private final EventCatalogPosterRepository posterRepository;
    private final EventRepository eventRepository;
    private final EventPriceBandRepository priceBandRepository;
    private final VenueRepository venueRepository;
    private final VenueSpaceRepository venueSpaceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EventCatalogResponse> getEvents() {
        return catalogRepository.findAllByActiveTrueOrderByTitleAsc().stream()
            .map(this::toCatalogResponse)
            .toList();
    }

    @Override
    @Transactional
    public EventCatalogResponse createEvent(EventCatalogRequest request, MultipartFile poster) {
        String title = request.getTitle().trim();
        String normalizedTitle = normalizeTitle(title);
        if (catalogRepository.findByNormalizedTitle(normalizedTitle).isPresent()) {
            throw new IllegalArgumentException("An event with this title already exists.");
        }

        EventCatalog event = new EventCatalog();
        applyCatalogRequest(event, request, title, normalizedTitle);
        EventCatalogPoster uploadedPoster = readPoster(poster);
        event.setPosterUploaded(uploadedPoster != null);
        if (uploadedPoster != null) {
            event.setPosterUrl(null);
        }
        EventCatalog savedEvent = catalogRepository.save(event);
        savePoster(savedEvent, uploadedPoster);
        return toCatalogResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventCatalogResponse updateEvent(Long id, EventCatalogRequest request, MultipartFile poster) {
        EventCatalog event = catalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        String title = request.getTitle().trim();
        String normalizedTitle = normalizeTitle(title);
        catalogRepository.findByNormalizedTitle(normalizedTitle)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("An event with this title already exists.");
            });
        applyCatalogRequest(event, request, title, normalizedTitle);
        EventCatalogPoster uploadedPoster = readPoster(poster);
        if (uploadedPoster != null) {
            event.setPosterUploaded(true);
            event.setPosterUrl(null);
        }
        EventCatalog savedEvent = catalogRepository.save(event);
        if (uploadedPoster != null) {
            savePoster(savedEvent, uploadedPoster);
        }
        return toCatalogResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventPoster getEventPoster(Long id) {
        EventCatalog event = catalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        if (!event.isPosterUploaded()) {
            throw new IllegalArgumentException("This event does not have an uploaded poster.");
        }
        EventCatalogPoster poster = posterRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("This event does not have an uploaded poster."));
        return new EventPoster(poster.getImageData(), poster.getContentType());
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        EventCatalog event = catalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        if (eventRepository.existsByCatalogEvent_Id(id)) {
            throw new IllegalArgumentException(
                "This event cannot be deleted because it is linked to one or more venue showings."
            );
        }
        posterRepository.deleteById(id);
        catalogRepository.delete(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowingResponse> getShowings() {
        List<Event> showings = isAdmin()
            ? eventRepository.findAllByOrderByStartTimeDesc()
            : eventRepository.findAllByVenue_Owner_EmailOrderByStartTimeDesc(currentEmail());
        if (showings.isEmpty()) {
            return List.of();
        }
        List<Long> showingIds = showings.stream().map(Event::getId).toList();
        Map<Long, List<EventPriceBandResponse>> bandsByShowing = new HashMap<>();
        priceBandRepository.findAllByEvent_IdInOrderByIdAsc(showingIds).forEach(band ->
            bandsByShowing.computeIfAbsent(band.getEvent().getId(), ignored -> new ArrayList<>())
                .add(toPriceBandResponse(band))
        );
        return showings.stream()
            .map(showing -> toShowingResponse(showing,
                bandsByShowing.getOrDefault(showing.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional
    public List<ShowingResponse> createShowings(ShowingsCreateRequest request) {
        Venue venue = venueRepository.findById(request.getVenueId())
            .orElseThrow(() -> new IllegalArgumentException("Venue not found."));
        if (Boolean.TRUE.equals(venue.getDeleteFlag())) {
            throw new IllegalArgumentException("Cannot schedule an event at a deleted venue.");
        }
        ensureVenueAccess(venue);
        EventCatalog catalogEvent = catalogRepository.findById(request.getEventId())
            .filter(EventCatalog::isActive)
            .orElseThrow(() -> new IllegalArgumentException("Selected catalog event is unavailable."));

        List<PreparedPerformance> prepared = new ArrayList<>(request.getPerformances().size());
        for (EventPerformanceRequest performance : request.getPerformances()) {
            validateTimes(performance.getStartTime(), performance.getEndTime());
            VenueSpace space = venueSpaceRepository
                .findByIdAndVenue_IdAndDeleteFlagFalse(performance.getVenueSpaceId(), venue.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Selected screen or stage does not belong to this active venue."
                ));
            validatePriceBands(space, performance.getPriceBands());
            prepared.add(new PreparedPerformance(performance, space));
        }
        validateNoOverlaps(prepared);

        List<ShowingResponse> created = new ArrayList<>(prepared.size());
        for (PreparedPerformance performance : prepared) {
            Event showing = new Event();
            showing.setTitle(catalogEvent.getTitle());
            showing.setCatalogEvent(catalogEvent);
            showing.setVenue(venue);
            showing.setVenueSpace(performance.space());
            showing.setStartTime(performance.request().getStartTime());
            showing.setEndTime(performance.request().getEndTime());
            Event savedShowing = eventRepository.save(showing);

            List<EventPriceBand> bands = performance.request().getPriceBands().stream()
                .map(bandRequest -> toPriceBand(savedShowing, bandRequest))
                .toList();
            priceBandRepository.saveAll(bands);
            List<EventPriceBandResponse> bandResponses = performance.request().getPriceBands().stream()
                .map(this::toPriceBandResponse)
                .toList();
            created.add(toShowingResponse(savedShowing, bandResponses));
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDiscoveryResponse> getDiscoverableEvents(String city) {
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = city == null || city.isBlank()
            ? eventRepository
                .findAllByVenue_DeleteFlagFalseAndVenueSpace_DeleteFlagFalseAndCatalogEvent_ActiveTrueAndStartTimeAfterOrderByStartTimeAsc(now)
            : eventRepository
                .findAllByVenue_CityIgnoreCaseAndVenue_DeleteFlagFalseAndVenueSpace_DeleteFlagFalseAndCatalogEvent_ActiveTrueAndStartTimeAfterOrderByStartTimeAsc(
                    city.trim(), now);
        if (upcoming.isEmpty()) {
            return List.of();
        }

        List<Long> showingIds = upcoming.stream().map(Event::getId).toList();
        Map<Long, List<EventPriceBandResponse>> bandsByShowing = new HashMap<>();
        priceBandRepository.findAllByEvent_IdInOrderByIdAsc(showingIds).forEach(band ->
            bandsByShowing.computeIfAbsent(band.getEvent().getId(), ignored -> new ArrayList<>())
                .add(toPriceBandResponse(band))
        );

        Map<Long, List<ShowingResponse>> showingsByCatalogEvent = new HashMap<>();
        Map<Long, EventCatalog> catalogEvents = new HashMap<>();
        for (Event showing : upcoming) {
            EventCatalog catalogEvent = showing.getCatalogEvent();
            catalogEvents.put(catalogEvent.getId(), catalogEvent);
            showingsByCatalogEvent.computeIfAbsent(catalogEvent.getId(), ignored -> new ArrayList<>())
                .add(toShowingResponse(showing, bandsByShowing.getOrDefault(showing.getId(), List.of())));
        }

        return catalogEvents.values().stream()
            .sorted(Comparator.comparing(EventCatalog::getTitle, String.CASE_INSENSITIVE_ORDER))
            .map(event -> new EventDiscoveryResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getPosterUrl(),
                event.getDurationMinutes(),
                showingsByCatalogEvent.get(event.getId())
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEventCities() {
        return venueRepository.findDistinctCityByDeleteFlagFalseOrderByCityAsc();
    }

    private void applyCatalogRequest(
            EventCatalog event,
            EventCatalogRequest request,
            String title,
            String normalizedTitle) {
        event.setTitle(title);
        event.setNormalizedTitle(normalizedTitle);
        event.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        event.setDurationMinutes(request.getDurationMinutes());
        event.setActive(true);
    }

    private EventCatalogPoster readPoster(MultipartFile poster) {
        if (poster == null || poster.isEmpty()) {
            return null;
        }
        if (poster.getSize() > MAX_POSTER_SIZE_BYTES) {
            throw new IllegalArgumentException("Poster image must be 5 MB or smaller.");
        }

        String contentType = poster.getContentType();
        if (!"image/jpeg".equalsIgnoreCase(contentType) && !"image/png".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("Poster image must be a JPEG or PNG file.");
        }

        try {
            byte[] data = poster.getBytes();
            validatePosterImage(data, contentType);
            return new EventCatalogPoster(
                null,
                data,
                contentType.toLowerCase(Locale.ROOT)
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read the poster image.", exception);
        }
    }

    private void savePoster(EventCatalog event, EventCatalogPoster poster) {
        if (poster == null) {
            return;
        }
        poster.setEventId(event.getId());
        posterRepository.save(poster);
    }

    private void validatePosterImage(byte[] data, String contentType) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (imageInput == null) {
                throw new IllegalArgumentException("Poster image is invalid or unsupported.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Poster image is invalid or unsupported.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean matchingFormat = "image/png".equalsIgnoreCase(contentType)
                    ? "png".equals(format)
                    : "jpeg".equals(format) || "jpg".equals(format);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!matchingFormat || width > MAX_POSTER_DIMENSION || height > MAX_POSTER_DIMENSION
                        || (long) width * height > MAX_POSTER_PIXELS) {
                    throw new IllegalArgumentException(
                        "Poster must be a valid JPEG or PNG image no larger than 8000 by 8000 pixels."
                    );
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be later than start time.");
        }
    }

    private void validateNoOverlaps(List<PreparedPerformance> performances) {
        for (int index = 0; index < performances.size(); index++) {
            PreparedPerformance current = performances.get(index);
            LocalDateTime start = current.request().getStartTime();
            LocalDateTime end = current.request().getEndTime();
            if (eventRepository.existsByVenueSpace_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                    current.space().getId(), end, start)) {
                throw new IllegalArgumentException(
                    "The selected screen or stage already has an event during this time."
                );
            }
            for (int otherIndex = 0; otherIndex < index; otherIndex++) {
                PreparedPerformance other = performances.get(otherIndex);
                if (current.space().getId().equals(other.space().getId())
                        && start.isBefore(other.request().getEndTime())
                        && end.isAfter(other.request().getStartTime())) {
                    throw new IllegalArgumentException(
                        "Two performances in this submission overlap on the same screen or stage."
                    );
                }
            }
        }
    }

    private void validatePriceBands(VenueSpace space, List<EventPriceBandRequest> bands) {
        if (bands == null || bands.isEmpty()) {
            throw new IllegalArgumentException("Add at least one price category for every performance.");
        }
        Set<String> categoryNames = new HashSet<>();
        for (EventPriceBandRequest band : bands) {
            if (!categoryNames.add(band.getCategoryName().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Price category names must be unique within a performance.");
            }
        }
        if (space.getSeatingMode() == SeatingMode.GENERAL_ADMISSION) {
            if (bands.size() != 1 || bands.get(0).getRowFrom() != null || bands.get(0).getRowTo() != null) {
                throw new IllegalArgumentException(
                    "General admission performances require exactly one price category without a row range."
                );
            }
            return;
        }
        if (space.getRowCount() == null) {
            throw new IllegalArgumentException("Reserved seating layout is missing its row count.");
        }
        List<EventPriceBandRequest> sortedBands = bands.stream()
            .sorted(Comparator.comparing(EventPriceBandRequest::getRowFrom,
                Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();
        int nextRow = 1;
        for (EventPriceBandRequest band : sortedBands) {
            if (band.getRowFrom() == null || band.getRowTo() == null
                    || band.getRowFrom() != nextRow || band.getRowTo() < band.getRowFrom()
                    || band.getRowTo() > space.getRowCount()) {
                throw new IllegalArgumentException(
                    "Price categories must cover every row exactly once without gaps or overlaps."
                );
            }
            nextRow = band.getRowTo() + 1;
        }
        if (nextRow != space.getRowCount() + 1) {
            throw new IllegalArgumentException(
                "Price categories must cover every row exactly once without gaps or overlaps."
            );
        }
    }

    private EventPriceBand toPriceBand(Event event, EventPriceBandRequest request) {
        EventPriceBand band = new EventPriceBand();
        band.setEvent(event);
        band.setCategoryName(request.getCategoryName().trim());
        band.setRowFrom(request.getRowFrom());
        band.setRowTo(request.getRowTo());
        band.setPrice(request.getPrice());
        return band;
    }

    private EventPriceBandResponse toPriceBandResponse(EventPriceBand band) {
        return new EventPriceBandResponse(band.getCategoryName(), band.getRowFrom(), band.getRowTo(), band.getPrice());
    }

    private EventPriceBandResponse toPriceBandResponse(EventPriceBandRequest band) {
        return new EventPriceBandResponse(band.getCategoryName().trim(), band.getRowFrom(), band.getRowTo(), band.getPrice());
    }

    private EventCatalogResponse toCatalogResponse(EventCatalog event) {
        return new EventCatalogResponse(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.isPosterUploaded() ? "/events/" + event.getId() + "/poster" : event.getPosterUrl(),
            event.getDurationMinutes()
        );
    }

    private ShowingResponse toShowingResponse(Event showing, List<EventPriceBandResponse> bands) {
        EventCatalog catalogEvent = showing.getCatalogEvent();
        return new ShowingResponse(
            showing.getId(),
            catalogEvent == null ? null : catalogEvent.getId(),
            catalogEvent == null ? showing.getTitle() : catalogEvent.getTitle(),
            catalogEvent == null ? null : toCatalogResponse(catalogEvent).posterUrl(),
            showing.getVenue().getId(),
            showing.getVenue().getName(),
            showing.getVenueSpace() == null ? null : showing.getVenueSpace().getId(),
            showing.getVenueSpace() == null ? null : showing.getVenueSpace().getName(),
            showing.getStartTime(),
            showing.getEndTime(),
            bands
        );
    }

    private String normalizeTitle(String title) {
        return title.trim().toLowerCase(Locale.ROOT);
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
            throw new AccessDeniedException("You do not have access to schedule events at this venue.");
        }
    }

    private record PreparedPerformance(EventPerformanceRequest request, VenueSpace space) {}
}
