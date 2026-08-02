package com.falconenergy.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoadingActivityWorkflowRequest {
    private String startedBy;
    private String completedBy;
}
