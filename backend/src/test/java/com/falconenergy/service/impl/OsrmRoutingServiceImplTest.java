package com.falconenergy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.falconenergy.config.RoutingProperties;
import com.falconenergy.dto.RouteResult;
import com.falconenergy.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OsrmRoutingServiceImplTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void selectsShortestRoadRouteAndReturnsProviderNeutralResult() throws Exception {
        OsrmRoutingServiceImpl service = new OsrmRoutingServiceImpl(new RoutingProperties(), mapper);
        RouteResult route = service.chooseShortest(mapper.readTree("""
                {"code":"Ok","routes":[
                  {"distance":18400.4,"duration":1320.1,"geometry":"long-route"},
                  {"distance":17400,"duration":1680,"geometry":"short-route"}
                ]}
                """));
        assertEquals(17400L, route.getDistanceMeters());
        assertEquals(new BigDecimal("17.400"), route.getDistanceKm());
        assertEquals(1680L, route.getDurationSeconds());
        assertEquals("short-route", route.getPolyline());
        assertEquals("OSRM", route.getProvider());
        assertEquals("SHORTEST_AVAILABLE", route.getRouteType());
    }

    @Test
    void noRouteResponseIsNotConvertedToAZeroDistance() throws Exception {
        OsrmRoutingServiceImpl service = new OsrmRoutingServiceImpl(new RoutingProperties(), mapper);
        assertNull(service.chooseShortest(mapper.readTree("{\"code\":\"NoRoute\",\"routes\":[]}")));
    }

    @Test
    void missingProviderConfigurationFailsClearly() {
        RoutingProperties properties = new RoutingProperties();
        properties.setBaseUrl("");
        properties.setFallbackBaseUrls(List.of());
        properties.setAllowEstimatedFallback(false);
        OsrmRoutingServiceImpl service = new OsrmRoutingServiceImpl(properties, mapper);
        assertThrows(BadRequestException.class, () -> service.calculateDrivingRoute(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));
    }

    @Test
    void usesFallbackProviderWhenPrimaryIsNotConfigured() {
        RoutingProperties properties = new RoutingProperties();
        properties.setBaseUrl("");
        properties.setFallbackBaseUrls(List.of("https://routing.example.test/", "https://routing.example.test"));

        OsrmRoutingServiceImpl service = new OsrmRoutingServiceImpl(properties, mapper);

        assertEquals(Set.of("https://routing.example.test"), service.configuredProviders());
    }

    @Test
    void returnsClearlyLabelledEstimateWhenAllProvidersAreUnavailable() {
        RoutingProperties properties = new RoutingProperties();
        properties.setBaseUrl("");
        properties.setFallbackBaseUrls(List.of());
        OsrmRoutingServiceImpl service = new OsrmRoutingServiceImpl(properties, mapper);

        RouteResult route = service.calculateDrivingRoute(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE);

        assertEquals("GEODESIC_FALLBACK", route.getProvider());
        assertEquals("ESTIMATED_STRAIGHT_LINE", route.getRouteType());
        assertEquals(new BigDecimal("138.994"), route.getDistanceKm());
    }
}
