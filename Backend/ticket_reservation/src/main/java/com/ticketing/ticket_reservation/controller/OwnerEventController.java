package com.ticketing.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.dto.EventCatalogResponse;
import com.ticketing.ticket_reservation.service.interfaces.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/owner/events")
@RequiredArgsConstructor
public class OwnerEventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventCatalogResponse>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }
}
