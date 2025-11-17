package com.gorap.rideservice.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface CreateRideProjection {
    UUID getId();
    String getSource();
    String getDestination();
    String getStatus();
    LocalDate getCreatedDate();
    LocalTime getRideTime();
}
