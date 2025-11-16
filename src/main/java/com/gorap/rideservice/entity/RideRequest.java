package com.gorap.rideservice.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.gorap.rideservice.constants.RideStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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


    @Enumerated(EnumType.STRING)
    private RideStatus rideStatus;


    private UUID driverId;  
    private String polyline;
    private Double distanceKm;
    private String duration;
    private int numberOfPassengers;

 

}
