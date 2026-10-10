package com.ticketing.ticket_reservation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPriceBandRequest {
    @NotBlank(message = "Price category is required.")
    @Size(max = 40, message = "Category name must be 40 characters or fewer.")
    private String categoryName;

    @Min(value = 1, message = "Starting row must be at least 1.")
    private Integer rowFrom;

    @Min(value = 1, message = "Ending row must be at least 1.")
    private Integer rowTo;

    @NotNull(message = "Seat price is required.")
    @DecimalMin(value = "0.00", message = "Price cannot be negative.")
    @Digits(integer = 8, fraction = 2, message = "Price can have up to 8 integer digits and 2 decimals.")
    private BigDecimal price;
}
