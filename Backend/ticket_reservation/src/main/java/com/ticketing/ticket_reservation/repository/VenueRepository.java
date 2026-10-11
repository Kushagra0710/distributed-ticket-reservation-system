package com.ticketing.ticket_reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ticketing.ticket_reservation.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findAllByDeleteFlagFalse();
    List<Venue> findAllByDeleteFlagFalseAndOwner_EmailOrderByNameAsc(String ownerEmail);
    @Query("""
        select distinct venue.city
        from Venue venue
        where venue.deleteFlag = false
          and venue.city is not null
          and trim(venue.city) <> ''
        order by venue.city asc
        """)
    List<String> findAvailableCities();
}
