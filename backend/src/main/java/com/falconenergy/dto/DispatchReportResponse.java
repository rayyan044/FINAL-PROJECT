package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchReportResponse {
    private Long totalDispatchedTrucks;
    private List<StatusCount> dispatchStatusDistribution;
    private List<DailyCount> dailyDispatchCount;
    private Long dispatchDelays;
    private LocalDate fromDate;
    private LocalDate toDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private String status;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private LocalDate date;
        private Long count;
    }
}
