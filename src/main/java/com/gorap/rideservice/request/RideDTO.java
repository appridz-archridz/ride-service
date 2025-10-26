package com.gorap.rideservice.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideDTO {

    private UUID id;

    private String startPoint;
    private Double startLatitude;
    private Double startLongitude;

    private String destinationPoint;
    private Double destinationLatitude;
    private Double destinationLongitude;

    private LocalDate rideDate;
    private LocalTime rideTime;

    private Integer availableSeats;
    
    private String polyline;

    private List<ViaPointDTO> viaPoints;
}
