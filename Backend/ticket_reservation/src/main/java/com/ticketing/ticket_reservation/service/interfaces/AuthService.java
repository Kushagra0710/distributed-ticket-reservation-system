package com.ticketing.ticket_reservation.service.interfaces;

import com.ticketing.ticket_reservation.dto.AuthRequest;
import com.ticketing.ticket_reservation.dto.AuthResponse;
import com.ticketing.ticket_reservation.dto.GoogleAuthRequest;
import com.ticketing.ticket_reservation.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    AuthResponse googleLogin(GoogleAuthRequest request);
}
