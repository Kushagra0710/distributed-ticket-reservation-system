package com.ticketing.ticket_reservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ticketing.ticket_reservation.service.interfaces.EventService;

@SpringBootTest
class TicketReservationApplicationTests {
	@Autowired
	private EventService eventService;

	@Test
	void contextLoads() {
	}

	@Test
	void publicDiscoveryQueriesExecute() {
		eventService.getEventCities();
		eventService.getDiscoverableEvents("Delhi");
		eventService.getDiscoverableShowings(1L, "Delhi");
	}

}
