package com.falconenergy.dto;

import com.falconenergy.entity.ReportStatus;
import com.falconenergy.entity.ReportType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSnapshotResponse {
    private Long id;
    private String reportNumber;
    private ReportType reportType;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private Map<String, Object> parameters;
    private String filePath;
    private ReportStatus status;
}
