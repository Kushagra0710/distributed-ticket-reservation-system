package com.ticketing.ticket_reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@RequiredArgsConstructor 
public class DeleteData {
    @NotBlank (message = "Delete flag is required.")
    Boolean deleteFlag;
}
