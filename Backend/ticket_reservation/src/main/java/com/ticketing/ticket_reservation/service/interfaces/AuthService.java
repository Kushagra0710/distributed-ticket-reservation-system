package com.ticketing.ticket_reservation.service.interfaces;

import com.ticketing.ticket_reservation.dto.AuthRequest;
import com.ticketing.ticket_reservation.dto.AuthResponse;
import com.ticketing.ticket_reservation.dto.GoogleAuthRequest;

public interface AuthService {
    AuthResponse register(AuthRequest request);
    AuthResponse login(AuthRequest request);
    AuthResponse googleLogin(GoogleAuthRequest request);
}
