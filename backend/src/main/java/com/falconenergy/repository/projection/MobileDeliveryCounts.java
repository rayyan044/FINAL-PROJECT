package com.falconenergy.repository.projection;

/** Aggregate values returned by one dashboard query, not an API entity. */
public record MobileDeliveryCounts(long deliveriesInProgress, long completedToday) {
}
