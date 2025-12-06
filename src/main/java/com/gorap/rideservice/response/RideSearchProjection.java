package com.gorap.rideservice.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.gorap.rideservice.constants.RideStatus;

public interface RideSearchProjection {

    String getUserName();
    String getPhoneNumber();
    UUID getRideId();
    String getStartLocation();
    String getEndLocation();
    LocalDate getRideDate();
    LocalTime getRideTime();
    Double getDistanceKm();
    String getDuration();
    Double getOfferedPrice();
    Integer getNumberOfPassengers();
    RideStatus getRideStatus();
}
