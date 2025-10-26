package com.gorap.rideservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gorap.rideservice.entity.CreateRide;



@Repository
public interface RideRepository extends JpaRepository<CreateRide, UUID> {

	// In RideRepository
	@Query("SELECT r FROM CreateRide r WHERE " +
	       "r.startLatitude BETWEEN :minLat AND :maxLat AND " +
	       "r.startLongitude BETWEEN :minLng AND :maxLng AND " +
	       "r.rideDate = :rideDate AND " +
	       "r.availableSeats > 0")
	List<CreateRide> findRidesInBoundingBox(
	    @Param("minLat") double minLat,
	    @Param("maxLat") double maxLat,
	    @Param("minLng") double minLng,
	    @Param("maxLng") double maxLng,
	    @Param("rideDate") LocalDate rideDate
	);
	
	@Query(value = "SELECT * FROM create_ride " +
            "WHERE start_latitude = :sourceLat " +
            "AND start_longitude = :sourceLng " +
            "AND destination_latitude = :destinationLat " +
            "AND destination_longitude = :destinationLng",
	    nativeQuery = true)
	List<CreateRide> findExactRides(
	 @Param("sourceLat") Double sourceLat,
	 @Param("sourceLng") Double sourceLng,
	 @Param("destinationLat") Double destinationLat,
	 @Param("destinationLng") Double destinationLng
	);

	@Query(value = "SELECT * FROM create_ride",
	    nativeQuery = true)
	List<CreateRide> findMiddleRides();
	
}
