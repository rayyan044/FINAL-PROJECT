package com.falconenergy.service.impl;

import com.falconenergy.dto.LoadingActivityWorkflowRequest;
import com.falconenergy.entity.LoadingActivity;
import com.falconenergy.entity.LoadingActivityStatus;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.LoadingActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadingActivityWorkflowServiceImplTest {
    @Mock private LoadingActivityRepository loadingActivityRepository;
    @InjectMocks private LoadingActivityWorkflowServiceImpl service;

    @Test
    void startsPendingActivityAndRecordsOperator() {
        LoadingActivity activity = LoadingActivity.builder().id(1L).truckNumber("124").status(LoadingActivityStatus.PENDING).build();
        when(loadingActivityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(loadingActivityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoadingActivityWorkflowRequest request = new LoadingActivityWorkflowRequest();
        request.setStartedBy("operation123@gmail.com");
        var response = service.start(1L, request);

        assertEquals("LOADING", response.getStatus());
        assertEquals("operation123@gmail.com", response.getStartedBy());
        assertNotNull(response.getLoadingStartedAt());
    }

    @Test
    void completesOnlyLoadingActivityAndRecordsOperator() {
        LoadingActivity activity = LoadingActivity.builder().id(1L).truckNumber("124").status(LoadingActivityStatus.LOADING).build();
        when(loadingActivityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(loadingActivityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoadingActivityWorkflowRequest request = new LoadingActivityWorkflowRequest();
        request.setCompletedBy("operation123@gmail.com");
        var response = service.complete(1L, request);

        assertEquals("LOADED", response.getStatus());
        assertEquals("operation123@gmail.com", response.getCompletedBy());
        assertNotNull(response.getLoadingCompletedAt());
    }

    @Test
    void rejectsCompletionBeforeLoadingStarts() {
        LoadingActivity activity = LoadingActivity.builder().id(1L).truckNumber("124").status(LoadingActivityStatus.PENDING).build();
        when(loadingActivityRepository.findById(1L)).thenReturn(Optional.of(activity));

        assertThrows(BadRequestException.class, () -> service.complete(1L, new LoadingActivityWorkflowRequest()));
    }
}
