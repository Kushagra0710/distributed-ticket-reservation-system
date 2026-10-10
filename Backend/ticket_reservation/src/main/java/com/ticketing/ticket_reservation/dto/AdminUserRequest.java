package com.ticketing.ticket_reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.ticketing.ticket_reservation.entity.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@RequiredArgsConstructor 
public class AdminUserRequest {
    
    @NotBlank (message = "Name is required")
    @Size(max=50, message="Name must be of max 50 characters")
    @Pattern (regexp = "^[\\p{L} ]+$", message = "Use letters and spaces only")
    private String fullName;

    @NotBlank (message = "Email is required")
    @Size(max=50, message="Email must be of max 50 characters")
    @Email (message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[1-9][0-9]{9}$", message = "Phone number must contain 10 digits")
    private String phoneNumber;

    private Long id;
    private Role role;

}
