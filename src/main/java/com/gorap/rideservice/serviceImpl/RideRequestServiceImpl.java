package com.gorap.rideservice.serviceImpl;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.repository.RideRequestRepository;
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

//    private double haversine(double lat1, double lon1, double lat2, double lon2) {
//        final int R = 6371; 
//        double dLat = Math.toRadians(lat2 - lat1);
//        double dLon = Math.toRadians(lon2 - lon1);
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
//                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        return R * c;
//    }

    @Override
    @Transactional
    public ResponseModel<RideRequest> createRideRequest(UUID userId, RideRequest rideRequest) {
        log.info("Begin RideRequestServiceImpl -> createRide() for user: {}", userId);

        ResponseModel<RideRequest> response = new ResponseModel<>();

        try {
            // Validate required fields
            if (rideRequest.getPolyline() == null || rideRequest.getPolyline().isEmpty()) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid path (polyline missing)");
            }

            if (!isValidCoordinates(rideRequest.getStartLatitude(), rideRequest.getStartLongitude()) ||
                !isValidCoordinates(rideRequest.getEndLatitude(), rideRequest.getEndLongitude())) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid coordinates provided");
            }


            if (rideRequest.getDistanceKm() < MIN_TRIP_DISTANCE_KM) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST,
                        String.format("Trip distance too short. Minimum: %.1f km", MIN_TRIP_DISTANCE_KM));
            }

            rideRequest.setCreatedBy(userId);

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
}
