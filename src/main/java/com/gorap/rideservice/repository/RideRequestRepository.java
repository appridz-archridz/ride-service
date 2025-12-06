package com.gorap.rideservice.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.response.RideSearchProjection;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {
	
	

	    @Query(
	        value = """
	            SELECT 
	                u.user_name AS userName,
	                u.phone_number AS phoneNumber,
	                r.id AS rideId,
	                r.start_location AS startLocation,
	                r.end_location AS endLocation,
	                r.ride_date AS rideDate,
	                r.ride_time AS rideTime,
	                r.distance_km AS distanceKm,
	                r.duration AS duration,
	                r.offered_price AS offeredPrice,
	                r.number_of_passengers AS numberOfPassengers
	            FROM ride.ride_request r
	            LEFT JOIN identity.users u ON u.id = r.created_by
	            WHERE (
	                    LOWER(r.start_location) LIKE LOWER(CONCAT('%', :city, '%'))
	                    OR LOWER(r.end_location)  LIKE LOWER(CONCAT('%', :city, '%'))
	                  )
	              AND (
	                      r.ride_date > CURRENT_DATE
	                   OR (r.ride_date = CURRENT_DATE AND r.ride_time > CURRENT_TIME)
	                  )
	            """,
	        countQuery = """
	            SELECT COUNT(*) 
	            FROM ride.ride_request r
	            WHERE (
	                    LOWER(r.start_location) LIKE LOWER(CONCAT('%', :city, '%'))
	                    OR LOWER(r.end_location)  LIKE LOWER(CONCAT('%', :city, '%'))
	                  )
	              AND (
	                      r.ride_date > CURRENT_DATE
	                   OR (r.ride_date = CURRENT_DATE AND r.ride_time > CURRENT_TIME)
	                  )
	            """,
	        nativeQuery = true
	    )
	    Page<RideSearchProjection> searchUpcomingRides(@Param("city") String city, Pageable pageable);
	    
	    
	    @Query(
	            value = """
	                    SELECT 
	                        u.user_name AS userName,
	                        u.phone_number AS phoneNumber,
	                        r.id AS rideId,
	                        r.start_location AS startLocation,
	                        r.end_location AS endLocation,
	                        r.ride_status AS rideStatus,
	                        r.ride_date AS rideDate,
	                        r.ride_time AS rideTime,
	                        r.distance_km AS distanceKm,
	                        r.duration AS duration,
	                        r.offered_price AS offeredPrice,
	                        r.number_of_passengers AS numberOfPassengers
	                    FROM ride.ride_request r
	                    LEFT JOIN identity.users u ON u.id = r.created_by
	                    WHERE r.created_by = :userId
	                    ORDER BY r.ride_date DESC, r.ride_time DESC
	                    """,
	            countQuery = """
	                    SELECT COUNT(*)
	                    FROM ride.ride_request r
	                    WHERE r.created_by = :userId
	                    """,
	            nativeQuery = true
	    )
	    Page<RideSearchProjection> findRequestsByUserId(@Param("userId") UUID userId, Pageable pageable);

	

}
