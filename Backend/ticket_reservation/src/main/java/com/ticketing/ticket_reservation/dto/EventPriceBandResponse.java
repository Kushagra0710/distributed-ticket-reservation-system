package com.ticketing.ticket_reservation.dto;

import java.math.BigDecimal;

public record EventPriceBandResponse(
    String categoryName,
    Integer rowFrom,
    Integer rowTo,
    BigDecimal price
) {}
