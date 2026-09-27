package com.ticketing.ticket_reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class AuthRequest {
    private String fullName;
    private String password; // optional for Google SSO

    @NotBlank(message="Email Id is required")
    @Email(message="Invalid Email format")
    private String email;
}
