package com.falconenergy.service;

import com.falconenergy.dto.DispatchResponse;
import com.falconenergy.dto.DispatchRequest;
import com.falconenergy.dto.LoadingActivityResponse;
import com.falconenergy.entity.LoadingActivity;
import com.falconenergy.entity.LoadingReport;
import java.util.List;

public interface DispatchService {
    List<LoadingActivityResponse> getPendingDispatchActivities();
    DispatchResponse createDispatch(Long loadingActivityId, DispatchRequest request);
    DispatchResponse createReadyDispatchForCompletedLoading(LoadingActivity activity, LoadingReport report);
    DispatchResponse getDispatchById(Long id);
    DispatchResponse getDispatchByActivityId(Long activityId);
    DispatchResponse releaseTruck(Long id);
    DispatchResponse startTransit(Long id);
}
