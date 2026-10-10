package com.ticketing.ticket_reservation.service.impl;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ticketing.ticket_reservation.entity.Event;
import com.ticketing.ticket_reservation.entity.EventCatalog;
import com.ticketing.ticket_reservation.repository.EventCatalogRepository;
import com.ticketing.ticket_reservation.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LegacyEventCatalogBackfill implements ApplicationRunner {
    private final EventRepository eventRepository;
    private final EventCatalogRepository catalogRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, EventCatalog> catalogsByTitle = new HashMap<>();
        catalogRepository.findAll().forEach(event ->
            catalogsByTitle.put(event.getNormalizedTitle(), event));
        List<Event> legacyShowings = eventRepository.findAll().stream()
            .filter(showing -> showing.getCatalogEvent() == null)
            .toList();

        for (var showing : legacyShowings) {
            String title = showing.getTitle().trim();
            String normalizedTitle = title.toLowerCase(Locale.ROOT);
            EventCatalog catalogEvent = catalogsByTitle.get(normalizedTitle);
            if (catalogEvent == null) {
                catalogEvent = new EventCatalog();
                catalogEvent.setTitle(title);
                catalogEvent.setNormalizedTitle(normalizedTitle);
                catalogEvent = catalogRepository.save(catalogEvent);
                catalogsByTitle.put(normalizedTitle, catalogEvent);
            }
            showing.setCatalogEvent(catalogEvent);
        }
        eventRepository.saveAll(legacyShowings);
    }
}
