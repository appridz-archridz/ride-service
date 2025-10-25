package com.gorap.rideservice.util;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@Service
public class RoutingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteResult getRoute(double originLat, double originLng, double destLat, double destLng) throws Exception {
        String url = String.format(
                "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=polyline",
                originLng, originLat, destLng, destLat
        );

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());

        JsonNode route = root.path("routes").get(0);
        String polyline = route.path("geometry").asText();
        double distanceMeters = route.path("distance").asDouble();

        return new RouteResult(polyline, distanceMeters / 1000.0); // km
    }

    @Getter
    public static class RouteResult {
        private final String polyline;
        private final double distanceKm;

        public RouteResult(String polyline, double distanceKm) {
            this.polyline = polyline;
            this.distanceKm = distanceKm;
        }
    }
}
