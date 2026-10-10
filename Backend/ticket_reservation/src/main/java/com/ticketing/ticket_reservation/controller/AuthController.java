package com.ticketing.ticket_reservation.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.constants.ApplicationConstants;
import com.ticketing.ticket_reservation.dto.ApiResponse;
import com.ticketing.ticket_reservation.dto.AuthRequest;
import com.ticketing.ticket_reservation.dto.AuthResponse;
import com.ticketing.ticket_reservation.dto.GoogleAuthRequest;
import com.ticketing.ticket_reservation.dto.RegisterRequest;
import com.ticketing.ticket_reservation.service.interfaces.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApplicationConstants.AUTH_BASE_URL)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(ApplicationConstants.REGISTER_URL)
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse(true, "Registration successful. Please sign in.", Map.of()));
    }

    @PostMapping(ApplicationConstants.LOGIN_URL)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(ApplicationConstants.GOOGLE)
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }
}