package com.ticketing.ticket_reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ticketing.ticket_reservation.entity.EventCatalog;

public interface EventCatalogRepository extends JpaRepository<EventCatalog, Long> {
    List<EventCatalog> findAllByActiveTrueOrderByTitleAsc();
    Optional<EventCatalog> findByNormalizedTitle(String normalizedTitle);

    @Query("""
        select distinct catalogEvent
        from Event showing
        join showing.catalogEvent catalogEvent
        join showing.venue venue
        join showing.venueSpace space
        where lower(venue.city) = lower(:city)
          and venue.deleteFlag = false
          and space.deleteFlag = false
          and catalogEvent.active = true
          and showing.startTime > :currentTime
        order by catalogEvent.title asc
        """)
    List<EventCatalog> findDiscoverableEventsByCity(
        @Param("city") String city,
        @Param("currentTime") java.time.LocalDateTime currentTime
    );
}
