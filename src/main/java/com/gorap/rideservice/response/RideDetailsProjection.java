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
    String getDestinationPoint();
}
