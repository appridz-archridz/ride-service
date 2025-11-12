package com.gorap.rideservice.response;

import java.util.UUID;

public interface CreateRideProjection {
    UUID getId();
    String getSource();
    String getDestination();
    String getStatus();
//    Timestamp getCreatedOn();
}
