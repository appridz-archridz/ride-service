package com.gorap.rideservice.service;

import java.util.List;
import java.util.UUID;

import com.gorap.rideservice.entity.VehicleInfo;
import com.gorap.rideservice.util.ResponseModel;

public interface VehicleInfoService {

    ResponseModel<VehicleInfo> createVehicle(UUID userId, VehicleInfo vehicleInfo);

    ResponseModel<VehicleInfo> getVehicle(UUID vehicleId);

    ResponseModel<VehicleInfo> updateVehicle(UUID vehicleId, VehicleInfo vehicleInfo);

    ResponseModel<String> deleteVehicle(UUID vehicleId);
    ResponseModel<List<VehicleInfo>> getAllVehiclesByUser(UUID userId);

}
