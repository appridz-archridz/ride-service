package com.gorap.rideservice.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@Table(name = "create_ride")
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

    @Lob
    private String polyline;  // Encoded polyline geometry

    private Double distanceKm; // Distance in km from OSRM

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ride_id")
    private List<ViaPoints> viaPoints;
}
