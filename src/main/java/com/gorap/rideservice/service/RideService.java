package com.gorap.rideservice.service;

import java.util.List;
import java.util.UUID;

import com.gorap.rideservice.entity.CreateRide;
import com.gorap.rideservice.request.RideDTO;
import com.gorap.rideservice.request.SearchRideDTO;
import com.gorap.rideservice.response.RideDetailsProjection;
import com.gorap.rideservice.util.ResponseModel;

public interface RideService {
    ResponseModel<CreateRide> createRide(RideDTO rideDTO, UUID userId);

	ResponseModel<List<CreateRide>> searchRides(SearchRideDTO searchRideDTO);
	ResponseModel<RideDetailsProjection> getRideDetails(UUID rideId);
}
