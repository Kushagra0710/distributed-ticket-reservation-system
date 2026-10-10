package com.ticketing.ticket_reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.ticket_reservation.entity.EventCatalogPoster;

public interface EventCatalogPosterRepository extends JpaRepository<EventCatalogPoster, Long> {}
