package com.falconenergy.service.impl;

import com.falconenergy.dto.LoadingActivityWorkflowRequest;
import com.falconenergy.dto.LoadingActivityWorkflowResponse;
import com.falconenergy.entity.LoadingActivity;
import com.falconenergy.entity.LoadingActivityStatus;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.LoadingActivityRepository;
import com.falconenergy.service.LoadingActivityWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoadingActivityWorkflowServiceImpl implements LoadingActivityWorkflowService {
    private final LoadingActivityRepository loadingActivityRepository;

    @Override
    public LoadingActivityWorkflowResponse start(Long activityId, LoadingActivityWorkflowRequest request) {
        LoadingActivity activity = find(activityId);
        if (activity.getStatus() != LoadingActivityStatus.PENDING) {
            throw new BadRequestException("Only pending loading activities can be started.");
        }
        String startedBy = actor(request == null ? null : request.getStartedBy());
        activity.setStatus(LoadingActivityStatus.LOADING);
        activity.setLoadingStartTime(LocalDateTime.now());
        activity.setStartedBy(startedBy);
        activity.setLoadingOfficer(startedBy);
        return response(loadingActivityRepository.save(activity));
    }

    @Override
    public LoadingActivityWorkflowResponse complete(Long activityId, LoadingActivityWorkflowRequest request) {
        LoadingActivity activity = find(activityId);
        if (activity.getStatus() != LoadingActivityStatus.LOADING) {
            throw new BadRequestException("Loading must be started before it can be completed.");
        }
        String completedBy = actor(request == null ? null : request.getCompletedBy());
        activity.setStatus(LoadingActivityStatus.LOADED);
        activity.setLoadingCompletionTime(LocalDateTime.now());
        activity.setCompletedAt(LocalDateTime.now());
        activity.setCompletedByName(completedBy);
        return response(loadingActivityRepository.save(activity));
    }

    private LoadingActivity find(Long id) {
        return loadingActivityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading activity not found with id: " + id));
    }

    private String actor(String requestedActor) {
        if (requestedActor != null && !requestedActor.trim().isEmpty()) return requestedActor.trim();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication.getName() : "system";
    }

    private LoadingActivityWorkflowResponse response(LoadingActivity activity) {
        return LoadingActivityWorkflowResponse.builder()
                .id(activity.getId())
                .truckNo(activity.getTruckNumber())
                .status(activity.getStatus().name())
                .loadingStartedAt(activity.getLoadingStartTime())
                .loadingCompletedAt(activity.getLoadingCompletionTime())
                .startedBy(activity.getStartedBy())
                .completedBy(activity.getCompletedByName())
                .build();
    }
}
