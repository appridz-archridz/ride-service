package com.gorap.rideservice.request;

import lombok.Data;

@Data
public class SearchRideDTO {
    private Double sourceLatitude;
    private Double sourceLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
}
