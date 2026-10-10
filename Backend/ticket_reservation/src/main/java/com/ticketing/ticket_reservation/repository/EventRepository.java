package com.ticketing.ticket_reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.ticket_reservation.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    List<Event> findAllByOrderByStartTimeDesc();

    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    List<Event> findAllByVenue_Owner_EmailOrderByStartTimeDesc(String ownerEmail);

    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    List<Event> findAllByVenue_DeleteFlagFalseAndVenueSpace_DeleteFlagFalseAndCatalogEvent_ActiveTrueAndStartTimeAfterOrderByStartTimeAsc(
        LocalDateTime currentTime
    );

    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    List<Event> findAllByVenue_CityIgnoreCaseAndVenue_DeleteFlagFalseAndVenueSpace_DeleteFlagFalseAndCatalogEvent_ActiveTrueAndStartTimeAfterOrderByStartTimeAsc(
        String city,
        LocalDateTime currentTime
    );

    boolean existsByVenueSpace_IdAndStartTimeLessThanAndEndTimeGreaterThan(
        Long venueSpaceId,
        LocalDateTime proposedEnd,
        LocalDateTime proposedStart
    );

    boolean existsByCatalogEvent_Id(Long catalogEventId);
}
