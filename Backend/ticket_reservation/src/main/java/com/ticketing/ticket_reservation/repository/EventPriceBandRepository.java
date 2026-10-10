package com.ticketing.ticket_reservation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.ticket_reservation.entity.EventPriceBand;

public interface EventPriceBandRepository extends JpaRepository<EventPriceBand, Long> {
    List<EventPriceBand> findAllByEvent_IdInOrderByIdAsc(Collection<Long> eventIds);
}
