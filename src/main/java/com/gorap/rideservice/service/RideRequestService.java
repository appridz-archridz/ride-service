package com.gorap.rideservice.service;

import java.util.UUID;

import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.util.ResponseModel;

public interface RideRequestService {

	ResponseModel<RideRequest> createRideRequest(UUID userId, RideRequest rideRequest);
}
