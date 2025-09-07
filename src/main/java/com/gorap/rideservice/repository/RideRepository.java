package com.gorap.rideservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gorap.rideservice.entity.CreateRide;



@Repository
public interface RideRepository extends JpaRepository<CreateRide, UUID> {
    // You can add custom query methods here if needed
}
