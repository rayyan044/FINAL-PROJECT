package com.falconenergy.service;

import com.falconenergy.dto.StorageTankRequest;
import com.falconenergy.dto.StorageTankResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface StorageTankService {
    StorageTankResponse createTank(StorageTankRequest request);
    StorageTankResponse getTankById(Long id);
    StorageTankResponse updateTank(Long id, StorageTankRequest request);
    void deleteTank(Long id);
    Page<StorageTankResponse> getAllTanks(String search, Pageable pageable);
    void adjustVolume(Long tankId, BigDecimal amount);
    void adjustVolume(Long tankId, BigDecimal amount, boolean isManual);
}
