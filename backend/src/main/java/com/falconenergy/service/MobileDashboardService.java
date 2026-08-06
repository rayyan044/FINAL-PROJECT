package com.falconenergy.service;

import com.falconenergy.dto.MobileDashboardResponse;
import java.util.List;

public interface MobileDashboardService {
    MobileDashboardResponse getDashboard();
    List<MobileDashboardResponse.RecentDelivery> getDeliveries();
    MobileDashboardResponse.RecentDelivery getDelivery(Long deliveryId);
}
