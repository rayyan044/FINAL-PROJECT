package com.falconenergy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchRequest {
    private Long loadingActivityId;
    private String remarks;
}
