package com.falconenergy.dto;
import java.math.BigDecimal;
import lombok.*;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RouteResult { private Long distanceMeters; private BigDecimal distanceKm; private Long durationSeconds; private String polyline; private String routeType; private String provider; }
