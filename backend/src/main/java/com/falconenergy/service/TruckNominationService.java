package com.falconenergy.service;

import com.falconenergy.dto.TruckNominationRequest;
import com.falconenergy.dto.TruckNominationResponse;

public interface TruckNominationService {
    TruckNominationResponse createNominationDraft(TruckNominationRequest request);
    TruckNominationResponse updateNominationDraft(Long id, TruckNominationRequest request);
    TruckNominationResponse getNominationById(Long id);
    TruckNominationResponse getNominationByOrderId(Long orderId);
    TruckNominationResponse submitNomination(Long id);
    void requestChanges(Long id, String reason);
    TruckNominationResponse approveNomination(Long id);
    TruckNominationResponse cancelNomination(Long id);
}
