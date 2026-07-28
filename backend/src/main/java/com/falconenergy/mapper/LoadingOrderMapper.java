package com.falconenergy.mapper;

import com.falconenergy.dto.LoadingActivityResponse;
import com.falconenergy.dto.LoadingOrderResponse;
import com.falconenergy.dto.LoadingOrderRequest;
import com.falconenergy.dto.LoadingCompartmentResponse;
import com.falconenergy.dto.LoadingReportResponse;
import com.falconenergy.entity.LoadingOrder;
import com.falconenergy.entity.LoadingActivity;
import com.falconenergy.entity.LoadingCompartment;
import com.falconenergy.entity.LoadingReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoadingOrderMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "customerOrderNumber", source = "order.orderNumber")
    @Mapping(target = "customerName", source = "order.customer.companyName")
    @Mapping(target = "product", source = "order.product.productName")
    @Mapping(target = "approvedQuantity", source = "order.approvedQuantity")
    @Mapping(target = "numberOfTrucks", expression = "java(entity.getActivities() != null ? entity.getActivities().size() : 0)")
    LoadingOrderResponse toResponse(LoadingOrder entity);

    @Mapping(target = "completedById", source = "completedBy.id")
    @Mapping(target = "completedByName", expression = "java(entity.getCompletedBy() != null ? entity.getCompletedBy().getFirstName() + \" \" + entity.getCompletedBy().getLastName() : null)")
    LoadingActivityResponse toActivityResponse(LoadingActivity entity);

    @Mapping(target = "productId", source = "product.id")
    LoadingCompartmentResponse toCompartmentResponse(LoadingCompartment entity);

    @Mapping(target = "loadingActivityId", source = "loadingActivity.id")
    @Mapping(target = "loadingOrderId", source = "loadingOrder.id")
    LoadingReportResponse toReportResponse(LoadingReport entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "activities", ignore = true)
    LoadingOrder toEntity(LoadingOrderRequest request);
}
