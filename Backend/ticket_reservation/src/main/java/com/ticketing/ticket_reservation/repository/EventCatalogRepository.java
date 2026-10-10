package com.ticketing.ticket_reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.ticket_reservation.entity.EventCatalog;

public interface EventCatalogRepository extends JpaRepository<EventCatalog, Long> {
    List<EventCatalog> findAllByActiveTrueOrderByTitleAsc();
    Optional<EventCatalog> findByNormalizedTitle(String normalizedTitle);
}
