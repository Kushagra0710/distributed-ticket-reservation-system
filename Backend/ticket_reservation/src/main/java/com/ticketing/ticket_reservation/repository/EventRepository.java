package com.ticketing.ticket_reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ticketing.ticket_reservation.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    List<Event> findAllByOrderByStartTimeDesc();

    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    List<Event> findAllByVenue_Owner_EmailOrderByStartTimeDesc(String ownerEmail);

    @EntityGraph(attributePaths = {"venue", "venueSpace", "catalogEvent"})
    @Query("""
        select showing
        from Event showing
        join showing.venue venue
        join showing.venueSpace space
        join showing.catalogEvent catalogEvent
        where catalogEvent.id = :catalogEventId
          and lower(venue.city) = lower(:city)
          and venue.deleteFlag = false
          and space.deleteFlag = false
          and catalogEvent.active = true
          and showing.startTime > :currentTime
        order by showing.startTime asc
        """)
    List<Event> findUpcomingShowingsForCatalogEvent(
        @Param("catalogEventId") Long catalogEventId,
        @Param("city") String city,
        @Param("currentTime") LocalDateTime currentTime
    );

    boolean existsByVenueSpace_IdAndStartTimeLessThanAndEndTimeGreaterThan(
        Long venueSpaceId,
        LocalDateTime proposedEnd,
        LocalDateTime proposedStart
    );

    boolean existsByCatalogEvent_Id(Long catalogEventId);
}
