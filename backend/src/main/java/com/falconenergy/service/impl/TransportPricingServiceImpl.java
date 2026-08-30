package com.falconenergy.service.impl;
import com.falconenergy.dto.*;
import com.falconenergy.entity.TransportDistanceRate;
import com.falconenergy.exception.*;
import com.falconenergy.repository.TransportDistanceRateRepository;
import com.falconenergy.service.TransportPricingService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class TransportPricingServiceImpl implements TransportPricingService {
  private final TransportDistanceRateRepository rates;
  @Override @Transactional(readOnly=true) public BigDecimal resolveDistancePrice(BigDecimal km) {
    List<TransportDistanceRate> matches=rates.matches(km);
    if(matches.size()!=1) throw new BadRequestException("Transport pricing is currently unavailable for this destination. Please contact Falcon support.");
    return matches.getFirst().getPrice();
  }
  @Override @Transactional(readOnly=true) public List<TransportDistanceRateResponse> all(){return rates.findAll().stream().map(this::map).toList();}
  @Override public TransportDistanceRateResponse create(TransportDistanceRateRequest r){ validate(r,null); return map(rates.save(TransportDistanceRate.builder().minimumKm(r.getMinimumKm()).maximumKm(r.getMaximumKm()).price(r.getPrice()).active(r.isActive()).build())); }
  @Override public TransportDistanceRateResponse update(Long id,TransportDistanceRateRequest r){ TransportDistanceRate x=rates.findById(id).orElseThrow(()->new ResourceNotFoundException("Distance transport rate not found")); validate(r,id); x.setMinimumKm(r.getMinimumKm());x.setMaximumKm(r.getMaximumKm());x.setPrice(r.getPrice());x.setActive(r.isActive());return map(rates.save(x)); }
  private void validate(TransportDistanceRateRequest r,Long id){if(r.getMaximumKm()!=null&&r.getMinimumKm().compareTo(r.getMaximumKm())>0)throw new BadRequestException("Minimum kilometres cannot exceed maximum kilometres.");if(r.isActive()&&rates.overlapsActive(r.getMinimumKm(),r.getMaximumKm(),id))throw new BadRequestException("This active distance range overlaps an existing active distance transport rate.");}
  private TransportDistanceRateResponse map(TransportDistanceRate x){return TransportDistanceRateResponse.builder().id(x.getId()).minimumKm(x.getMinimumKm()).maximumKm(x.getMaximumKm()).price(x.getPrice()).active(x.isActive()).build();}
}
