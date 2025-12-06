package com.gorap.rideservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gorap.rideservice.entity.CreateRide;
import com.gorap.rideservice.response.CreateRideProjection;
import com.gorap.rideservice.response.RideDetailsProjection;
import com.gorap.rideservice.response.RideSearchProjection;



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
	
	
    @Query(value = """
            SELECT 
                r.id AS id,
                u.phone_number AS phoneNumber,
                u.profile_pic AS profilePic,
                u.user_name AS userName,
                r.ride_date AS rideDate,
                r.ride_time AS rideTime,
                r.distance_km AS distanceKm,
                r.start_point AS startPoint,
                r.destination_point AS destinationPoint
            FROM ride.create_ride r
            LEFT JOIN identity.users u ON u.id = r.created_by
            WHERE r.id = :rideId
        """, nativeQuery = true)
        Optional<RideDetailsProjection> findRideDetailsById(UUID rideId);

	@Query(value = "SELECT * FROM create_ride",
	    nativeQuery = true)
	List<CreateRide> findMiddleRides();
	
	
	@Query(value = """
		    SELECT 
		        id AS id,
		        start_point AS source,
		        destination_point AS destination,
		        'ACTIVE' AS status,
		        ride_date AS createdDate,
		        ride_time AS rideTime
		    FROM ride.create_ride
		    WHERE created_by = :userId
		""", nativeQuery = true)
		List<CreateRideProjection> findRidesByCreatedBy(@Param("userId") UUID userId);
	
	


	
	
}
