package com.ecoamazonas.eco_agua.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.maps.api-key}")
    private String apiKey;

    public Optional<LatLng> geocode(String address, String reference) {
        if ((address == null || address.isBlank())
                && (reference == null || reference.isBlank())) {
            return Optional.empty();
        }

        String fullQuery = (address != null ? address : "");
        if (reference != null && !reference.isBlank()) {
            fullQuery = fullQuery + " " + reference;
        }

        // Help Google by adding city/country context
        fullQuery = fullQuery + ", Iquitos, Peru";

        URI uri = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", fullQuery)
                .queryParam("key", apiKey)
                .build(true)
                .toUri();

        ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Optional.empty();
        }

        Map<String, Object> body = response.getBody();
        String status = (String) body.get("status");
        if (!"OK".equals(status)) {
            return Optional.empty();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> first = results.get(0);
        Map<String, Object> geometry = (Map<String, Object>) first.get("geometry");
        if (geometry == null) {
            return Optional.empty();
        }

        Map<String, Object> location = (Map<String, Object>) geometry.get("location");
        if (location == null) {
            return Optional.empty();
        }

        Double lat = (Double) location.get("lat");
        Double lng = (Double) location.get("lng");
        if (lat == null || lng == null) {
            return Optional.empty();
        }

        return Optional.of(new LatLng(lat, lng));
    }

    public static class LatLng {
        private final double lat;
        private final double lng;

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }
    }
}
