package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadingReportAnalyticsResponse {
    private Long totalTrucksLoaded;
    private BigDecimal totalAmbientVolume;
    private BigDecimal totalStandardVolume;
    private Double averageLoadingDuration; // in minutes
    private Double loadingCompletionPercentage;
    private LocalDate fromDate;
    private LocalDate toDate;
}
