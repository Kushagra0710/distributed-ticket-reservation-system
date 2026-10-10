package com.ticketing.ticket_reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.ticket_reservation.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findAllByDeleteFlagFalse();
    List<Venue> findAllByDeleteFlagFalseAndOwner_EmailOrderByNameAsc(String ownerEmail);
    List<String> findDistinctCityByDeleteFlagFalseOrderByCityAsc();
}
