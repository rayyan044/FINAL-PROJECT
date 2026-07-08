package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageTankResponse {
    private Long id;
    private String tankName;
    private BigDecimal capacity;
    private BigDecimal currentVolume;
    private FuelProductResponse fuelProduct;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
