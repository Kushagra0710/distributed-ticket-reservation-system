package com.ticketing.ticket_reservation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "event_price_bands",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "category_name"})
)
@Getter
@Setter
@NoArgsConstructor
public class EventPriceBand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "category_name", nullable = false, length = 40)
    private String categoryName;

    @Column(name = "row_from")
    private Integer rowFrom;

    @Column(name = "row_to")
    private Integer rowTo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
