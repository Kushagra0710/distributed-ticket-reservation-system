package com.ticketing.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.dto.ShowingResponse;
import com.ticketing.ticket_reservation.dto.ShowingsCreateRequest;
import com.ticketing.ticket_reservation.service.interfaces.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ShowingController {
    private final EventService eventService;

    @GetMapping("/api/admin/showings")
    public ResponseEntity<List<ShowingResponse>> getAdminShowings() {
        return ResponseEntity.ok(eventService.getShowings());
    }

    @GetMapping("/api/owner/showings")
    public ResponseEntity<List<ShowingResponse>> getOwnerShowings() {
        return ResponseEntity.ok(eventService.getShowings());
    }

    @PostMapping("/api/admin/showings")
    public ResponseEntity<List<ShowingResponse>> createAdminShowings(
            @Valid @RequestBody ShowingsCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createShowings(request));
    }

    @PostMapping("/api/owner/showings")
    public ResponseEntity<List<ShowingResponse>> createOwnerShowings(
            @Valid @RequestBody ShowingsCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createShowings(request));
    }
}
