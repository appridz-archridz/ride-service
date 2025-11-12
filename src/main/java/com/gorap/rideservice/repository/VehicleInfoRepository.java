package com.gorap.rideservice.repository;

import com.gorap.rideservice.entity.VehicleInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehicleInfoRepository extends JpaRepository<VehicleInfo, UUID> {
}
