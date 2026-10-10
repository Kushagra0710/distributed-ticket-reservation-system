package com.ticketing.ticket_reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_catalog_poster")
@Getter
@Setter
@NoArgsConstructor
public class EventCatalogPoster {
    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "image_data", nullable = false, columnDefinition = "bytea")
    private byte[] imageData;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    public EventCatalogPoster(Long eventId, byte[] imageData, String contentType) {
        this.eventId = eventId;
        this.imageData = imageData;
        this.contentType = contentType;
    }
}
