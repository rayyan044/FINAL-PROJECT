package com.falconenergy.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geocoding")
@Getter
@Setter
public class GeocodingProperties {
    private String openMeteoBaseUrl = "https://geocoding-api.open-meteo.com";
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";
    private int timeoutSeconds = 5;

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
