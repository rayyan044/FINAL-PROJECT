package com.falconenergy.service;

import com.falconenergy.dto.FuelOrderRequest;
import com.falconenergy.dto.FuelOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface FuelOrderService {
    FuelOrderResponse createOrder(FuelOrderRequest request);
    FuelOrderResponse getOrderById(Long id);
    FuelOrderResponse updateOrder(Long id, FuelOrderRequest request);
    FuelOrderResponse updateOrderStatus(Long id, String status);
    FuelOrderResponse approveOrderWithEdit(Long id, com.falconenergy.dto.FuelOrderEditApprovalRequest request);
    void deleteOrder(Long id);
    Page<FuelOrderResponse> getAllOrders(String search, String status, Long customerId, Long productId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
