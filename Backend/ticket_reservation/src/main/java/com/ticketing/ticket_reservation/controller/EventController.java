package com.ticketing.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.dto.EventCatalogRequest;
import com.ticketing.ticket_reservation.dto.EventCatalogResponse;
import com.ticketing.ticket_reservation.service.interfaces.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventCatalogResponse>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventCatalogResponse> createEvent(
            @Valid @RequestPart("event") EventCatalogRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, poster));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventCatalogResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestPart("event") EventCatalogRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, poster));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
