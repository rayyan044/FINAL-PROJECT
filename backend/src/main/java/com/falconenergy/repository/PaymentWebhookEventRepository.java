package com.falconenergy.repository;

import com.falconenergy.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
 boolean existsByGatewayAndEventId(String gateway, String eventId);
}
