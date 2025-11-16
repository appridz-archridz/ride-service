package com.gorap.rideservice.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gorap.rideservice.constants.RideStatus;
import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.repository.RideRequestRepository;
import com.gorap.rideservice.request.RideSearchRequest;
import com.gorap.rideservice.response.RideSearchProjection;
import com.gorap.rideservice.service.RideRequestService;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideRequestServiceImpl implements RideRequestService {

    private final RideRequestRepository rideRequestRepository;

    private static final double MIN_TRIP_DISTANCE_KM = 0.5;

    private boolean isValidCoordinates(Double lat, Double lon) {
        return lat != null && lon != null && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    @Override
    @Transactional
    public ResponseModel<RideRequest> createRideRequest(UUID userId, RideRequest rideRequest) {
        log.info("Begin RideRequestServiceImpl -> createRide() for user: {}", userId);

        ResponseModel<RideRequest> response = new ResponseModel<>();

        try {
   

            if (!isValidCoordinates(rideRequest.getStartLatitude(), rideRequest.getStartLongitude()) ||
                !isValidCoordinates(rideRequest.getEndLatitude(), rideRequest.getEndLongitude())) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid coordinates provided");
            }


            if (rideRequest.getDistanceKm() < MIN_TRIP_DISTANCE_KM) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST,
                        String.format("Trip distance too short. Minimum: %.1f km", MIN_TRIP_DISTANCE_KM));
            }

            rideRequest.setCreatedBy(userId);
            rideRequest.setRideStatus(RideStatus.open);

            RideRequest savedRide = rideRequestRepository.save(rideRequest);

            response.setStatusCode(String.valueOf(HttpStatus.CREATED.toString()));
            response.setMessage("Ride created successfully");
            response.setData(savedRide);
            response.setSuccess(true);

            log.info("RideRequest created successfully with ID: {}", savedRide.getId());

        } catch (Exception e) {
            log.error("Error creating ride request: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create ride request: " + e.getMessage());
        }

        return response;
    }

    private ResponseModel<RideRequest> createErrorResponse(ResponseModel<RideRequest> response, HttpStatus status, String message) {
        response.setStatusCode(status.toString());
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }
    
    @Override
    public ResponseModel<List<RideSearchProjection>> searchRides(RideSearchRequest searchRideDTO) {
        log.info("Begin RideServiceImpl -> searchRides()");
        ResponseModel<List<RideSearchProjection>> response = new ResponseModel<>();

        try {
            String city = searchRideDTO.getCity() == null ? "" : searchRideDTO.getCity().toLowerCase();
            int page = searchRideDTO.getPageNumber() == null ? 0 : searchRideDTO.getPageNumber();
            int size = searchRideDTO.getPageSize() == null ? 10 : searchRideDTO.getPageSize();

            Pageable pageable = PageRequest.of(page, size, Sort.by("rideDate").ascending());

            Page<RideSearchProjection> result = rideRequestRepository.searchUpcomingRides(city, pageable);

            if (result.isEmpty()) {
                response.setStatusCode(String.valueOf(HttpStatus.NOT_FOUND.value()));
                response.setMessage("No upcoming rides found for city: " + city);
                log.info("End RideServiceImpl -> searchRides()");
                return response;
            }

            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("Rides fetched successfully");
            response.setData(result.getContent());

        } catch (Exception e) {
            log.error("Error in RideServiceImpl -> searchRides() ", e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage("Failed to fetch rides: " + e.getMessage());
        }

        log.info("End RideServiceImpl -> searchRides()");
        return response;
    }
    
    
    @Override
    @Transactional
    public ResponseModel<RideRequest> updateRideRequest(UUID rideId, RideRequest updatedRideRequest) {
        log.info("Begin RideRequestServiceImpl -> updateRide() for rideId: {}", rideId);

        ResponseModel<RideRequest> response = new ResponseModel<>();

        try {
          System.out.println("ride Request"+ updatedRideRequest);
            RideRequest existingRide = rideRequestRepository.findById(rideId)
                    .orElse(null);

            if (existingRide == null) {
                return createErrorResponse(response, HttpStatus.NOT_FOUND,
                        "Ride request not found with ID: " + rideId);
            }

            // 2. Validations
            if (!isValidCoordinates(updatedRideRequest.getStartLatitude(), updatedRideRequest.getStartLongitude()) ||
                !isValidCoordinates(updatedRideRequest.getEndLatitude(), updatedRideRequest.getEndLongitude())) {

                return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid coordinates provided");
            }

            if (updatedRideRequest.getDistanceKm() < MIN_TRIP_DISTANCE_KM) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST,
                        String.format("Trip distance too short. Minimum: %.1f km", MIN_TRIP_DISTANCE_KM));
            }

            // 3. Update only allowed fields
            existingRide.setStartLocation(updatedRideRequest.getStartLocation());
            existingRide.setEndLocation(updatedRideRequest.getEndLocation());

            existingRide.setStartLatitude(updatedRideRequest.getStartLatitude());
            existingRide.setStartLongitude(updatedRideRequest.getStartLongitude());
            existingRide.setEndLatitude(updatedRideRequest.getEndLatitude());
            existingRide.setEndLongitude(updatedRideRequest.getEndLongitude());

            existingRide.setRideDate(updatedRideRequest.getRideDate());
            existingRide.setRideTime(updatedRideRequest.getRideTime());

            existingRide.setOfferedPrice(updatedRideRequest.getOfferedPrice());
            existingRide.setDistanceKm(updatedRideRequest.getDistanceKm());
            existingRide.setDuration(updatedRideRequest.getDuration());
            existingRide.setRideStatus(RideStatus.open);
            existingRide.setNumberOfPassengers(updatedRideRequest.getNumberOfPassengers());

            existingRide.setPolyline(updatedRideRequest.getPolyline());
            existingRide.setRideStatus(updatedRideRequest.getRideStatus());

            // 4. Optional: assign driver (only if provided)
            if (updatedRideRequest.getDriverId() != null) {
                existingRide.setDriverId(updatedRideRequest.getDriverId());
            }

            RideRequest savedRide = rideRequestRepository.save(existingRide);

            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("Ride updated successfully");
            response.setData(savedRide);
            response.setSuccess(true);

            log.info("RideRequest updated successfully with ID: {}", savedRide.getId());

        } catch (Exception e) {
            log.error("Error updating ride request: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update ride request: " + e.getMessage());
        }

        return response;
    }
    
    
    @Override
    @Transactional
    public ResponseModel<RideRequest> cancelRide(UUID rideId) {
        log.info("Begin RideRequestServiceImpl -> cancelRide() for rideId: {}", rideId);

        ResponseModel<RideRequest> response = new ResponseModel<>();

        try {
            RideRequest existingRide = rideRequestRepository.findById(rideId)
                    .orElse(null);

            if (existingRide == null) {
                return createErrorResponse(response, HttpStatus.NOT_FOUND,
                        "Ride not found with ID: " + rideId);
            }

            // If already canceled
            if (existingRide.getRideStatus() == RideStatus.cancelled) {
                response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
                response.setMessage("Ride already cancelled");
                response.setData(existingRide);
                response.setSuccess(true);
                return response;
            }

            existingRide.setRideStatus(RideStatus.cancelled);

            RideRequest savedRide = rideRequestRepository.save(existingRide);

            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("Ride cancelled successfully");
            response.setData(savedRide);
            response.setSuccess(true);

            log.info("RideRequest cancelled successfully with ID: {}", rideId);

        } catch (Exception e) {
            log.error("Error cancelling ride request: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to cancel ride: " + e.getMessage());
        }

        return response;
    }
    
    
    @Override
    public ResponseModel<RideRequest> getRideById(UUID rideId) {
        log.info("Begin RideRequestServiceImpl -> getRideById() for rideId: {}", rideId);

        ResponseModel<RideRequest> response = new ResponseModel<>();

        try {
            RideRequest existingRide = rideRequestRepository.findById(rideId)
                    .orElse(null);

            if (existingRide == null) {
                return createErrorResponse(response, HttpStatus.NOT_FOUND,
                        "Ride not found with ID: " + rideId);
            }

            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("Ride fetched successfully");
            response.setData(existingRide);
            response.setSuccess(true);

            log.info("RideRequest fetched successfully with ID: {}", rideId);

        } catch (Exception e) {
            log.error("Error fetching ride request: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch ride: " + e.getMessage());
        }

        return response;
    }
    
    @Override
    public ResponseModel<List<RideSearchProjection>> myRequestedRides(RideSearchRequest request) {
        log.info("Fetching ride requests for userId: {}");
        ResponseModel<List<RideSearchProjection>> response = new ResponseModel<>();

        try {
        	
        	 int page = request.getPageNumber() == null ? 0 : request.getPageNumber();
             int size = request.getPageSize() == null ? 10 : request.getPageSize();

             Pageable pageable = PageRequest.of(page, size, Sort.by("rideDate").ascending());
            Page<RideSearchProjection> rides= rideRequestRepository.findRequestsByUserId(request.getUserId(), pageable);

            if (rides.isEmpty()) {
                response.setStatusCode(String.valueOf(HttpStatus.NOT_FOUND.value()));
                response.setMessage("No ride requests found for user: " + request.getUserId());
                return response;
            }

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setMessage("Ride requests fetched successfully");
            response.setData(rides.getContent());

        } catch (Exception e) {
            log.error("Error fetching ride requests by userId: ", e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage("Failed to fetch ride requests: " + e.getMessage());
        }

        return response;
    }





}
