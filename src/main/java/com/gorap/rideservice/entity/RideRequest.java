package com.gorap.rideservice.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ride_request",schema="ride")
@Getter
@Setter
public class RideRequest  extends BaseEntity{

    

    private String startLocation;
    private String endLocation;

    private Double startLatitude;
    private Double startLongitude;
    private Double endLatitude;
    private Double endLongitude;

    private LocalDate rideDate;
    private LocalTime rideTime;

    private Double offeredPrice;
//    private Double acceptedPrice;

//    @Enumerated(EnumType.STRING)
//    private RideStatus status = RideStatus.PENDING;


    private UUID driverId;  
    private String polyline;
    private String vehicleType;
    private Double distanceKm;
    private String duration;
    private int numberOfPassengers;

 

}
