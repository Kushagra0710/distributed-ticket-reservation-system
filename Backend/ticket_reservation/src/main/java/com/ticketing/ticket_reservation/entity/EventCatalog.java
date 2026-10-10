package com.ticketing.ticket_reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "event_catalog",
    uniqueConstraints = @UniqueConstraint(name = "uk_event_catalog_normalized_title", columnNames = "normalized_title")
)
@Getter
@Setter
@NoArgsConstructor
public class EventCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "normalized_title", nullable = false, length = 255)
    private String normalizedTitle;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "poster_url", length = 2048)
    private String posterUrl;

    @Column(name = "poster_uploaded", nullable = false, columnDefinition = "boolean not null default false")
    private boolean posterUploaded;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(nullable = false)
    private boolean active = true;
}
