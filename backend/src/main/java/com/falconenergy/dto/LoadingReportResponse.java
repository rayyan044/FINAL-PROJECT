package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingReportResponse {
    private Long id;
    private Long loadingActivityId;
    private Long loadingOrderId;
    private String reportNumber;
    private String loadingOfficer;
    private String terminal;
    private String loadingBay;
    private String reportStatus;
    private LocalDateTime createdAt;
}
