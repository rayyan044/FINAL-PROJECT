package com.falconenergy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.falconenergy.config.RoutingProperties;
import com.falconenergy.dto.RouteResult;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.service.RoutingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OSRM adapter. Requests alternatives and uses the shortest valid returned driving route. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OsrmRoutingServiceImpl implements RoutingService {
    private final RoutingProperties properties;
    private final ObjectMapper mapper;

    @Override
    public RouteResult calculateDrivingRoute(BigDecimal originLatitude, BigDecimal originLongitude,
                                             BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        Set<String> providers = configuredProviders();
        if (providers.isEmpty() && !properties.isAllowEstimatedFallback()) {
            throw new BadRequestException("Routing service is unavailable. Please contact Falcon support.");
        }
        boolean receivedValidResponse = false;
        for (String provider : providers) {
            try {
                HttpResponse<String> response = requestRoute(provider, originLatitude, originLongitude, destinationLatitude, destinationLongitude);
                if (response.statusCode() / 100 != 2) {
                    log.warn("Routing provider returned HTTP {}: {}", response.statusCode(), provider);
                    continue;
                }
                receivedValidResponse = true;
                RouteResult route = chooseShortest(mapper.readTree(response.body()));
                if (route != null) return route;
            } catch (Exception exception) {
                log.warn("Routing provider request failed: {} ({})", provider, exception.getMessage());
            }
        }
        if (receivedValidResponse) {
            throw new BadRequestException("No driving route was found for this delivery destination.");
        }
        if (properties.isAllowEstimatedFallback()) {
            log.warn("All routing providers are unavailable; using an estimated distance for order continuity");
            return estimateRoute(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        }
        throw new BadRequestException("Routing service is unavailable. Please try again.");
    }

    Set<String> configuredProviders() {
        Set<String> providers = new LinkedHashSet<>();
        addProvider(providers, properties.getBaseUrl());
        List<String> fallbacks = properties.getFallbackBaseUrls();
        if (fallbacks != null) fallbacks.forEach(fallback -> addProvider(providers, fallback));
        return providers;
    }

    private void addProvider(Set<String> providers, String provider) {
        if (provider != null && !provider.isBlank()) providers.add(provider.replaceAll("/+$", ""));
    }

    private HttpResponse<String> requestRoute(String baseUrl, BigDecimal originLatitude, BigDecimal originLongitude,
                                              BigDecimal destinationLatitude, BigDecimal destinationLongitude) throws Exception {
        String coordinates = originLongitude.toPlainString() + "," + originLatitude.toPlainString()
                + ";" + destinationLongitude.toPlainString() + "," + destinationLatitude.toPlainString();
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coordinates
                + "?alternatives=true&overview=full&geometries=polyline");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build();
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private RouteResult estimateRoute(BigDecimal originLatitude, BigDecimal originLongitude,
                                      BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        double latitudeDelta = Math.toRadians(destinationLatitude.doubleValue() - originLatitude.doubleValue());
        double longitudeDelta = Math.toRadians(destinationLongitude.doubleValue() - originLongitude.doubleValue());
        double originLatitudeRadians = Math.toRadians(originLatitude.doubleValue());
        double destinationLatitudeRadians = Math.toRadians(destinationLatitude.doubleValue());
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(originLatitudeRadians) * Math.cos(destinationLatitudeRadians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        // A 25% uplift produces a conservative road-distance estimate from the direct geodesic distance.
        long meters = Math.max(1, Math.round(6_371_008.8 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine)) * 1.25));
        return RouteResult.builder()
                .distanceMeters(meters)
                .distanceKm(BigDecimal.valueOf(meters).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP))
                .durationSeconds(Math.max(60, Math.round(meters / 1000d / 45d * 3600d)))
                .provider("GEODESIC_FALLBACK")
                .routeType("ESTIMATED_STRAIGHT_LINE")
                .build();
    }

    // Package-visible for deterministic adapter tests without a live routing provider.
    RouteResult chooseShortest(JsonNode response) {
        if (!"Ok".equals(response.path("code").asText())) return null;
        JsonNode routes = response.path("routes");
        if (!routes.isArray() || routes.isEmpty()) return null;
        RouteResult best = null;
        for (JsonNode route : routes) {
            long meters = Math.round(route.path("distance").asDouble(-1));
            long seconds = Math.round(route.path("duration").asDouble(-1));
            if (meters <= 0 || seconds < 0) continue;
            RouteResult candidate = RouteResult.builder()
                    .distanceMeters(meters)
                    .distanceKm(BigDecimal.valueOf(meters).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP))
                    .durationSeconds(seconds)
                    .polyline(route.path("geometry").asText(null))
                    .provider("OSRM")
                    .routeType("SHORTEST_AVAILABLE")
                    .build();
            if (best == null || candidate.getDistanceMeters() < best.getDistanceMeters()) best = candidate;
        }
        return best;
    }
}
