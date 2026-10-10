package com.ticketing.ticket_reservation.service.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ticketing.ticket_reservation.dto.EventCatalogRequest;
import com.ticketing.ticket_reservation.dto.EventCatalogResponse;
import com.ticketing.ticket_reservation.dto.EventDiscoveryResponse;
import com.ticketing.ticket_reservation.dto.EventPoster;
import com.ticketing.ticket_reservation.dto.ShowingsCreateRequest;
import com.ticketing.ticket_reservation.dto.ShowingResponse;

public interface EventService {
    List<EventCatalogResponse> getEvents();
    EventCatalogResponse createEvent(EventCatalogRequest request, MultipartFile poster);
    EventCatalogResponse updateEvent(Long id, EventCatalogRequest request, MultipartFile poster);
    EventPoster getEventPoster(Long id);
    void deleteEvent(Long id);
    List<ShowingResponse> getShowings();
    List<ShowingResponse> createShowings(ShowingsCreateRequest request);
    List<EventDiscoveryResponse> getDiscoverableEvents(String city);
    List<String> getEventCities();
}
