package com.gorap.rideservice.response;

import java.util.UUID;

public interface RideDetailsProjection {

	UUID getId();
    String getPhoneNumber();
    String getProfilePic();
    String getUserName();
    String getRideDate();
    String getRideTime();
    Double getDistanceKm();
    String getStartPoint();
    Double getStartLatitude();
    Double getStartLongitude();
    String getDestinationPoint();
    Double getDestinationLatitude();
    Double getDestinationLongitude();
    String getVehicleNumber();
    String getVehicleType();
    UUID getVehicleId();
}
