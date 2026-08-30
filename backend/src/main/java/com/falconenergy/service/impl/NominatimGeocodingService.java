package com.falconenergy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.falconenergy.config.GeocodingProperties;
import com.falconenergy.dto.GeocodingLocationResponse;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.service.GeocodingService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NominatimGeocodingService implements GeocodingService {
    private final GeocodingProperties properties;
    private final ObjectMapper mapper;

    @Override
    public List<GeocodingLocationResponse> search(String query) {
        if (query == null || query.trim().length() < 3) return List.of();
        List<GeocodingLocationResponse> openMeteoResults = searchOpenMeteo(query.trim());
        if (!openMeteoResults.isEmpty()) return openMeteoResults;

        JsonNode response = request(properties.getNominatimBaseUrl(), "/search?format=jsonv2&addressdetails=1&limit=5&countrycodes=tz&q="
                + encode(query.trim()));
        List<GeocodingLocationResponse> locations = new ArrayList<>();
        if (response.isArray()) {
            for (JsonNode item : response) location(item).ifPresent(locations::add);
        }
        return locations;
    }

    @Override
    public GeocodingLocationResponse reverse(BigDecimal latitude, BigDecimal longitude) {
        return location(request(properties.getNominatimBaseUrl(), "/reverse?format=jsonv2&lat=" + latitude + "&lon=" + longitude))
                .orElse(new GeocodingLocationResponse("Selected map location", latitude, longitude));
    }

    private List<GeocodingLocationResponse> searchOpenMeteo(String query) {
        try {
            JsonNode results = request(properties.getOpenMeteoBaseUrl(), "/v1/search?name=" + encode(query + ", Tanzania")
                    + "&count=8&language=en&format=json").path("results");
            if (!results.isArray()) return List.of();

            List<GeocodingLocationResponse> locations = new ArrayList<>();
            for (JsonNode item : results) {
                if (!"TZ".equalsIgnoreCase(item.path("country_code").asText())) continue;
                String address = String.join(", ", List.of(
                        item.path("name").asText(), item.path("admin1").asText(), item.path("country").asText()
                ).stream().filter(value -> !value.isBlank()).toList());
                locations.add(new GeocodingLocationResponse(address,
                        BigDecimal.valueOf(item.path("latitude").asDouble()),
                        BigDecimal.valueOf(item.path("longitude").asDouble())));
            }
            return locations;
        } catch (BadRequestException exception) {
            return List.of();
        }
    }

    private JsonNode request(String baseUrl, String path) {
        try {
            URI uri = URI.create(baseUrl.replaceAll("/+$", "") + path);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(properties.timeout())
                    .header("Accept", "application/json")
                    .header("User-Agent", "FalconFuelManagement/1.0 (location search)")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(properties.timeout())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Geocoding provider returned HTTP {}", response.statusCode());
                throw new BadRequestException("Location search is temporarily unavailable.");
            }
            return mapper.readTree(response.body());
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Geocoding provider request failed: {}", exception.getMessage());
            throw new BadRequestException("Location search is temporarily unavailable.");
        }
    }

    private java.util.Optional<GeocodingLocationResponse> location(JsonNode item) {
        try {
            String address = item.path("display_name").asText();
            BigDecimal latitude = item.path("lat").decimalValue();
            BigDecimal longitude = item.path("lon").decimalValue();
            return address.isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(new GeocodingLocationResponse(address, latitude, longitude));
        } catch (Exception exception) {
            return java.util.Optional.empty();
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
