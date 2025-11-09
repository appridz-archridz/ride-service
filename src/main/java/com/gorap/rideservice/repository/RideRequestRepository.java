package com.gorap.rideservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gorap.rideservice.entity.RideRequest;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {
}
