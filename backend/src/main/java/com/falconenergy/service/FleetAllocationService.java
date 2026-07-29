package com.falconenergy.service;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Vehicle;
import java.util.List;
public interface FleetAllocationService {
    List<Vehicle> suggest(FuelOrder order);
}
