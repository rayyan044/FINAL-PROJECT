package com.falconenergy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuration for the road-routing provider. OSRM can be public for light local testing or self-hosted. */
@Getter
@Setter
@ConfigurationProperties(prefix = "routing")
public class RoutingProperties {
    private String baseUrl = "https://router.project-osrm.org";
    /**
     * OSRM-compatible endpoints to try when the primary provider is temporarily unavailable.
     * Production deployments should replace the public defaults with managed or self-hosted endpoints.
     */
    private List<String> fallbackBaseUrls = List.of("https://routing.openstreetmap.de/routed-car");
    /** Keep order placement available during an outage, while marking the saved route as an estimate. */
    private boolean allowEstimatedFallback = true;
    private int timeoutSeconds = 12;
}
