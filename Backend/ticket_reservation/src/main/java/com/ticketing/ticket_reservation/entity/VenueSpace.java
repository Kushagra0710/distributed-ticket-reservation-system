package com.ticketing.ticket_reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "venue_spaces",
    uniqueConstraints = @UniqueConstraint(columnNames = {"venue_id", "name"}),
    indexes = @Index(name = "idx_venue_space_venue", columnList = "venue_id, delete_flag")
)
@Getter
@Setter
@NoArgsConstructor
public class VenueSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false, length = 20)
    private SpaceType spaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "seating_mode", nullable = false, length = 24)
    private SeatingMode seatingMode;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "seats_per_row")
    private Integer seatsPerRow;

    @NotNull
    @Column(name = "delete_flag", nullable = false)
    private Boolean deleteFlag = false;

}
