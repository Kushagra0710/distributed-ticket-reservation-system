package com.ticketing.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.dto.EventDiscoveryResponse;
import com.ticketing.ticket_reservation.dto.EventPoster;
import com.ticketing.ticket_reservation.service.interfaces.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EventDiscoveryController {
    private final EventService eventService;

    @GetMapping("/api/events")
    public ResponseEntity<List<EventDiscoveryResponse>> getEvents(
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(eventService.getDiscoverableEvents(city));
    }

    @GetMapping("/api/events/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(eventService.getEventCities());
    }

    @GetMapping("/api/events/{id}/poster")
    public ResponseEntity<byte[]> getPoster(@PathVariable Long id) {
        EventPoster poster = eventService.getEventPoster(id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .contentType(MediaType.parseMediaType(poster.contentType()))
            .body(poster.data());
    }
}
