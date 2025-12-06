package com.gorap.rideservice.request;

import java.util.UUID;

import com.gorap.rideservice.constants.RideStatus;

import lombok.Data;
@Data
public class RideUpdateDto {
	private UUID id;
     private RideStatus rideStatus;
     private Integer availableSeats;
}
