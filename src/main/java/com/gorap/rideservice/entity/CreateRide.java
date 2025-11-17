package com.gorap.rideservice.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.gorap.rideservice.constants.RideStatus;
import com.gorap.rideservice.constants.VehicleType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "create_ride",schema="ride")
public class CreateRide extends BaseEntity {

    private String startPoint;
    private Double startLatitude;
    private Double startLongitude;

    private String destinationPoint;
    private Double destinationLatitude;
    private Double destinationLongitude;

    private LocalDate rideDate;
    private LocalTime rideTime;

    private Integer availableSeats;
    
    @Enumerated(EnumType.STRING)
    private RideStatus rideStatus;
    
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;
    
    @Column(columnDefinition = "TEXT")
    private String polyline;

    private Double distanceKm; // Distance in km from OSRM

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ride_id")
    private List<ViaPoints> viaPoints;
}
