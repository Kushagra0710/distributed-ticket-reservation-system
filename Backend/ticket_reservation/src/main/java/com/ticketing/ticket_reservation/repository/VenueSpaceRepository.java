package com.ticketing.ticket_reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.ticket_reservation.entity.VenueSpace;

public interface VenueSpaceRepository extends JpaRepository<VenueSpace, Long> {
    List<VenueSpace> findAllByVenue_IdAndDeleteFlagFalseOrderByIdAsc(Long venueId);
    Optional<VenueSpace> findByIdAndVenue_IdAndDeleteFlagFalse(Long id, Long venueId);
    List<VenueSpace> findAllByVenue_IdAndDeleteFlagFalseOrderByNameAsc(Long venueId);
    boolean existsByVenue_IdAndNameIgnoreCase(Long venueId, String name);
    boolean existsByVenue_IdAndNameIgnoreCaseAndIdNot(Long venueId, String name, Long id);
}
