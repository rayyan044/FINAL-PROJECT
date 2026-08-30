package com.falconenergy.service;

import com.falconenergy.dto.RouteResult;
import java.math.BigDecimal;

/** Provider-neutral road routing boundary. Distances are always driving-route distances. */
public interface RoutingService {
    RouteResult calculateDrivingRoute(
            BigDecimal originLatitude, BigDecimal originLongitude,
            BigDecimal destinationLatitude, BigDecimal destinationLongitude);
}
