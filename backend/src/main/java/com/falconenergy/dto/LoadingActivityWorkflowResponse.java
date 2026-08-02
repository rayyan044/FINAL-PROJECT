package com.falconenergy.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class LoadingActivityWorkflowResponse {
    private Long id;
    private String truckNo;
    private String status;
    private LocalDateTime loadingStartedAt;
    private LocalDateTime loadingCompletedAt;
    private String startedBy;
    private String completedBy;
}
