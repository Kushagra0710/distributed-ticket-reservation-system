package com.ticketing.ticket_reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueRequest {
    private Long id;

    @NotBlank (message = "Venue name is required.")
    @Size(max = 50, message = "Name must be 50 characters or fewer")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Use letters and spaces only")
    private String name;

    @NotBlank (message = "Address name is required.")
    @Size(max = 50, message = "Address must be 50 characters or fewer")
    private String address;

    @NotNull(message = "Pincode is required.")
    @Min(value = 100000, message = "Pincode must be of 6 digits")
    @Max(value = 999999, message = "Pincode must be of 6 digits")
    private Integer pincode;

    @NotBlank(message = "Venue type is required.")
    @Pattern(regexp = "^(Theatre|Concert Ground|Auditorium)$", message = "Venue type must be Theatre, Concert Ground, or Auditorium.")
    private String venueType;

    private Long ownerId;
}
