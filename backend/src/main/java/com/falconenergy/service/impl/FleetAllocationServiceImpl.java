package com.falconenergy.service.impl;

import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.LoadingActivityRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.service.FleetAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

/** Selects a compatible fleet combination: least excess capacity, then fewest trucks, then largest capacities. */
@Service @RequiredArgsConstructor
public class FleetAllocationServiceImpl implements FleetAllocationService {
 private final VehicleRepository vehicles;
 private final LoadingActivityRepository activities;
 @Override public List<Vehicle> suggest(FuelOrder order) {
  String fuelType=order.getProduct().getFuelType();
  List<Vehicle> candidates=vehicles.findAll().stream()
    .filter(Vehicle::isActive)
    .filter(v->"AVAILABLE".equalsIgnoreCase(v.getCurrentStatus()) || "ACTIVE".equalsIgnoreCase(v.getCurrentStatus()))
    .filter(v->activities.findActiveByVehicleId(v.getId()).isEmpty())
    .filter(v->v.getAssignedFuelTypes().stream().anyMatch(f->f.equalsIgnoreCase(fuelType)))
    .sorted(Comparator.comparing(Vehicle::getCapacity).reversed()).toList();
  if(candidates.isEmpty()) throw new BadRequestException("No available vehicle has been configured to transport this fuel product.");
  Best best=new Best(order.getQuantity()); search(candidates,0,new ArrayList<>(),BigDecimal.ZERO,best);
  if(best.selection==null) throw new BadRequestException("No compatible available fleet combination can satisfy " + order.getQuantity() + " L of " + fuelType);
  return best.selection;
 }
 private void search(List<Vehicle> xs,int at,List<Vehicle> picked,BigDecimal sum,Best best) {
  if(sum.compareTo(best.required)>=0){best.consider(picked,sum);return;}
  if(at==xs.size()) return;
  if(best.selection!=null && picked.size()>=best.selection.size()) return;
  Vehicle v=xs.get(at); picked.add(v);search(xs,at+1,picked,sum.add(v.getCapacity()),best);picked.remove(picked.size()-1);
  search(xs,at+1,picked,sum,best);
 }
 private static class Best { final BigDecimal required; List<Vehicle> selection; BigDecimal total;
  Best(BigDecimal r){required=r;}
  void consider(List<Vehicle> candidate,BigDecimal sum){
   if(selection==null || better(candidate,sum)){selection=new ArrayList<>(candidate);total=sum;}
  }
  boolean better(List<Vehicle> c,BigDecimal sum){int x=sum.subtract(required).compareTo(total.subtract(required));if(x!=0)return x<0;if(c.size()!=selection.size())return c.size()<selection.size();
   List<BigDecimal>a=c.stream().map(Vehicle::getCapacity).sorted(Comparator.reverseOrder()).toList(),b=selection.stream().map(Vehicle::getCapacity).sorted(Comparator.reverseOrder()).toList();for(int i=0;i<a.size();i++){int z=a.get(i).compareTo(b.get(i));if(z!=0)return z>0;}return false;
  }
 }
}
