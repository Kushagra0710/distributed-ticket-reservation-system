package com.ticketing.ticket_reservation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name; // e.g., "AMC Empire Screen 1"

    @NotBlank
    @Column(nullable = false)
    private String location; // e.g., "New York, NY"

    @Column(name = "total_capacity")
    private Integer totalCapacity;
}