package com.ticketing.ticket_reservation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@NoArgsConstructor 
@Builder 
@AllArgsConstructor 
@Table (name="seats")
public class Seat {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY) @Column (name="id")
    private Long id;

    @Column(name="seat_number", unique = true, nullable=false)
    private String seatNumber ;

    @Enumerated (EnumType.STRING) @Column (name="seat_status",nullable=false)
    private SeatStatus seatStatus;

    @Column (name="price",nullable=false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name="event_id",nullable = false)
    private Event event;

    @Version private Long version; // optimistic concurrency guard


}
