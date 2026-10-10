package com.ticketing.ticket_reservation.utility;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.ticketing.ticket_reservation.dto.PincodeLocation;

@Component
public class PostalPincodeUtil {

    private static final String POSTAL_API_URL = "https://api.postalpincode.in/pincode/{pincode}";
    private static final Pattern PINCODE_PATTERN = Pattern.compile("\\d{6}");

    private final ConcurrentMap<String, PincodeLocation> locationCache = new ConcurrentHashMap<>();
    private final RestClient restClient;

    public PostalPincodeUtil(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = restClientBuilder
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .build();
    }

    public PincodeLocation getLocationByPincode(String pincode) {
        if (pincode == null || !PINCODE_PATTERN.matcher(pincode).matches()) {
            throw new IllegalArgumentException("Pincode must be exactly 6 digits");
        }

        return locationCache.computeIfAbsent(pincode, this::fetchLocation);
    }

    private PincodeLocation fetchLocation(String pincode) {
        JsonNode response = restClient.get()
            .uri(POSTAL_API_URL, pincode)
            .retrieve()
            .body(JsonNode.class);

        JsonNode result = response == null || !response.isArray() || response.isEmpty()
            ? null
            : response.get(0);
        JsonNode postOffices = result == null ? null : result.get("PostOffice");

        if (result == null
                || !"Success".equals(result.path("Status").asText())
                || postOffices == null
                || !postOffices.isArray()
                || postOffices.isEmpty()) {
            throw new IllegalArgumentException("No location found for pincode " + pincode);
        }

        JsonNode postOffice = postOffices.get(0);
        String city = postOffice.path("District").asText();
        String state = postOffice.path("State").asText();
        if (city.isBlank() || state.isBlank()) {
            throw new IllegalStateException("Postal API response did not include city and state");
        }

        return new PincodeLocation(city, state);
    }
}
