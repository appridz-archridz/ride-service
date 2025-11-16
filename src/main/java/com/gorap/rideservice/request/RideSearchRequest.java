package com.gorap.rideservice.request;

import java.util.UUID;

import lombok.Data;

@Data
public class RideSearchRequest {
    private String city;
    private Integer pageNumber;
    private Integer pageSize;
    private UUID userId;
}
