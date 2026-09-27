package com.ticketing.ticket_reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class GoogleAuthRequest {
    @NotBlank (message="Google ID token is required")
    private String idToken;
}
