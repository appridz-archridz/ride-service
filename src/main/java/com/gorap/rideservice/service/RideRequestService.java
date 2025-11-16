package com.gorap.rideservice.service;

import java.util.List;
import java.util.UUID;

import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.request.RideSearchRequest;
import com.gorap.rideservice.response.RideSearchProjection;
import com.gorap.rideservice.util.ResponseModel;

public interface RideRequestService {

	ResponseModel<RideRequest> createRideRequest(UUID userId, RideRequest rideRequest);

	ResponseModel<List<RideSearchProjection>> searchRides(RideSearchRequest searchRideDTO);

	ResponseModel<RideRequest> updateRideRequest(UUID rideId, RideRequest rideRequest);

	ResponseModel<RideRequest> cancelRide(UUID rideId);

	ResponseModel<RideRequest> getRideById(UUID rideId);



	ResponseModel<List<RideSearchProjection>> myRequestedRides(RideSearchRequest searchRideDTO);
}
