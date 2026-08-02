package com.falconenergy.service;

import com.falconenergy.dto.LoadingActivityWorkflowRequest;
import com.falconenergy.dto.LoadingActivityWorkflowResponse;

public interface LoadingActivityWorkflowService {
    LoadingActivityWorkflowResponse start(Long activityId, LoadingActivityWorkflowRequest request);
    LoadingActivityWorkflowResponse complete(Long activityId, LoadingActivityWorkflowRequest request);
}
