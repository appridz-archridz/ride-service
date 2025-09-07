package com.gorap.rideservice.service;

import java.util.UUID;

import com.gorap.rideservice.entity.CreateRide;
import com.gorap.rideservice.request.RideDTO;
import com.gorap.rideservice.util.ResponseModel;

public interface RideService {
    ResponseModel<CreateRide> createRide(RideDTO rideDTO, UUID userId);
}
