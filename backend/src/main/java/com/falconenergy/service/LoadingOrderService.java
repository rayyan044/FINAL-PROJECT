package com.falconenergy.service;

import com.falconenergy.dto.LoadingOrderRequest;
import com.falconenergy.dto.LoadingOrderResponse;
import java.util.List;

public interface LoadingOrderService {
    LoadingOrderResponse createLoadingOrder(LoadingOrderRequest request);
    LoadingOrderResponse updateLoadingOrder(Long id, LoadingOrderRequest request);
    LoadingOrderResponse getLoadingOrderById(Long id);
    LoadingOrderResponse getLoadingOrderByOrderId(Long orderId);
    List<LoadingOrderResponse> getAllLoadingOrders();
    LoadingOrderResponse approveLoadingOrder(Long id);
    LoadingOrderResponse cancelLoadingOrder(Long id);
    LoadingOrderResponse startLoadingActivity(Long id, Long activityId, String bayNumber, String pumpNumber);
    LoadingOrderResponse completeLoadingActivity(Long id, Long activityId);
}
