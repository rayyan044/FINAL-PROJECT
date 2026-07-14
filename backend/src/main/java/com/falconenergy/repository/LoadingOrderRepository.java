package com.falconenergy.repository;

import com.falconenergy.entity.LoadingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LoadingOrderRepository extends JpaRepository<LoadingOrder, Long> {
    Optional<LoadingOrder> findByOrderId(Long orderId);
    Optional<LoadingOrder> findByLoadingOrderNumber(String loadingOrderNumber);

    @Query(value = "SELECT loading_order_number FROM loading_orders WHERE loading_order_number LIKE concat(:prefix, '%') ORDER BY loading_order_number DESC LIMIT 1", nativeQuery = true)
    String findMaxOrderNumberWithPrefix(@Param("prefix") String prefix);
}
