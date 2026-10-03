package com.ticketing.ticket_reservation.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private Map<String, String> errors;
}