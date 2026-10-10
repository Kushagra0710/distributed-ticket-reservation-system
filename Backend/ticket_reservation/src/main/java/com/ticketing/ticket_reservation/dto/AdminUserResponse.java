package com.ticketing.ticket_reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminUserResponse {
    private boolean success;
    private String message;
}
