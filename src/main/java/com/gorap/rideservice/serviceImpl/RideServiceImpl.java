package com.gorap.rideservice.serviceImpl;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gorap.rideservice.constants.RideStatus;
import com.gorap.rideservice.entity.CreateRide;
import com.gorap.rideservice.entity.ViaPoints;
import com.gorap.rideservice.exception.RecordNotFoundException;
import com.gorap.rideservice.repository.RideRepository;
import com.gorap.rideservice.request.RideDTO;
import com.gorap.rideservice.request.RideUpdateDto;
import com.gorap.rideservice.request.SearchRideDTO;
import com.gorap.rideservice.response.CreateRideProjection;
import com.gorap.rideservice.response.RideDetailsProjection;
import com.gorap.rideservice.service.RideService;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;

    // Configuration constants
    private static final double TOLERANCE_KM = 2.0; // Distance tolerance for route matching
    private static final double MIN_TRIP_DISTANCE_KM = 0.5; // Minimum viable trip distance
    private static final double SEARCH_RADIUS_KM = 50.0; // Search bounding box radius
    private static final int EARTH_RADIUS_KM = 6371; // Earth radius for Haversine formula

    @Override
    @Transactional
    public ResponseModel<CreateRide> createRide(RideDTO rideDTO, UUID userId) {
        log.info("Creating ride for user: {}", userId);
        ResponseModel<CreateRide> response = new ResponseModel<>();
        
        try {
        	
        	if (rideDTO.getPolyline() == "" || rideDTO.getPolyline() == null) {
        		return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid path");
        	}
        	
        	
            // Validate coordinates
            if (!isValidCoordinates(rideDTO.getStartLatitude(), rideDTO.getStartLongitude()) ||
                !isValidCoordinates(rideDTO.getDestinationLatitude(), rideDTO.getDestinationLongitude())) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid coordinates provided");
            }
 
            // Validate via points coordinates
            if (rideDTO.getViaPoints() != null) {
                for (var viaPoint : rideDTO.getViaPoints()) {
                    if (!isValidCoordinates(viaPoint.getPickupLatitude(), viaPoint.getPickupLongitude()) ||
                        !isValidCoordinates(viaPoint.getDropLatitude(), viaPoint.getDropLongitude())) {
                        return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid via point coordinates");
                    }
                }
            }

            // Validate minimum trip distance
            double tripDistance = haversine(
                rideDTO.getStartLatitude(), rideDTO.getStartLongitude(),
                rideDTO.getDestinationLatitude(), rideDTO.getDestinationLongitude()
            );
            
            if (tripDistance < MIN_TRIP_DISTANCE_KM) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST, 
                    String.format("Trip distance too short. Minimum: %.1f km", MIN_TRIP_DISTANCE_KM));
            }
            
            CreateRide ride = mapToEntity(rideDTO);
            ride.setCreatedBy(userId);

            ride.setPolyline(rideDTO.getPolyline());

            CreateRide saved = rideRepository.save(ride);

            response.setStatusCode(String.valueOf(HttpStatus.CREATED.value()));
            response.setMessage("Ride created successfully");
            response.setData(saved);
            response.setSuccess(true);
            
            log.info("Ride created successfully with ID: {} with polyline: {}", 
                saved.getId(), saved.getPolyline() != null ? "Yes" : "No");
            
        } catch (Exception e) {
            log.error("Error creating ride: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to create ride: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseModel<List<CreateRide>> searchRides(SearchRideDTO searchRideDTO) {
        log.info("Searching rides from ({},{}) to ({},{})", 
            searchRideDTO.getSourceLatitude(), searchRideDTO.getSourceLongitude(),
            searchRideDTO.getDestinationLatitude(), searchRideDTO.getDestinationLongitude());
        
        ResponseModel<List<CreateRide>> response = new ResponseModel<>();
        
        try {
            // Validate coordinates
            if (!isValidCoordinates(searchRideDTO.getSourceLatitude(), searchRideDTO.getSourceLongitude()) ||
                !isValidCoordinates(searchRideDTO.getDestinationLatitude(), searchRideDTO.getDestinationLongitude())) {
                return createErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid search coordinates");
            }

            	List<CreateRide> allRides = rideRepository.findAll(
            	    // searchRideDTO.getLocalDate()
            	); 
            	
            	System.out.println("all rides are " + allRides.size());

            	List<CreateRide> candidateMiddleRides = new ArrayList<>();
            	

            	for (CreateRide ride : allRides) {
            	    String polyline = ride.getPolyline();

            	    boolean isRideMatch =  isRouteMatching(polyline, searchRideDTO.getSourceLatitude(), searchRideDTO.getSourceLongitude(), searchRideDTO.getDestinationLatitude(), searchRideDTO.getDestinationLongitude());
            	    
	            	if (isRideMatch) {
	            		candidateMiddleRides.add(ride);
	            	}
            	}
            	List<CreateRide> candidateRides = new ArrayList<>();
            	candidateRides.addAll(candidateMiddleRides);
            
            log.debug("Found {} candidate rides in bounding box", candidateRides.size());
            System.out.println(candidateMiddleRides.size());
            response.setSuccess(true);
            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setMessage(String.format("Found %d matching rides", candidateRides.size()));
            response.setData(candidateMiddleRides);
            
        } catch (Exception e) {
            log.error("Error searching rides: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to search rides: " + e.getMessage());
        }
        
        return response;
    }
    
    public static boolean isRouteMatching(String polyline, double startLat, double startLng, double endLat, double endLng) {
//    	log.info("Begin RideServiceImpl -> isRouteMatching()" + polyline);
        String[] points = polyline.split(";");
        boolean sourceFound = false;
        double tolerance = 0.001;

        for (String point : points) {
            String[] latLng = point.split(",");
            if (latLng.length != 2) continue; // skip invalid points

            double lat = Double.parseDouble(latLng[0]);
            double lng = Double.parseDouble(latLng[1]);
            
            if (!sourceFound) {
                // Check for start point match
                if ((Math.abs(startLat - lat) <= tolerance && Math.abs(startLng - lng) <= tolerance)) {
                    sourceFound = true;
                }
            } else {
                // After source found, check for end point match
                if (Math.abs(endLat - lat) <= tolerance && Math.abs(endLng - lng) <= tolerance) {
                    return true; // both start and end found in correct order
                }
            }
        }

        return false; // no full match found
    }


    private RouteMatch findClosestPointOnRoute(List<double[]> routePoints, double lat, double lng) {
        RouteMatch bestMatch = null;
        double cumulativeDistance = 0.0;
        
        for (int i = 0; i < routePoints.size() - 1; i++) {
            double[] p1 = routePoints.get(i);
            double[] p2 = routePoints.get(i + 1);
            
            PointProjection projection = projectPointOnSegment(p1, p2, lat, lng);
            
            if (projection.distance <= TOLERANCE_KM) {
                double routeDistanceToPoint = cumulativeDistance + projection.distanceAlongSegment;
                
                if (bestMatch == null || projection.distance < bestMatch.distanceToRoute) {
                    bestMatch = new RouteMatch(i, projection.t, projection.distance, routeDistanceToPoint);
                }
            }
            
            cumulativeDistance += haversine(p1[0], p1[1], p2[0], p2[1]);
        }
        
        // Check last point
        double[] lastPoint = routePoints.get(routePoints.size() - 1);
        double distanceToLast = haversine(lastPoint[0], lastPoint[1], lat, lng);
        if (distanceToLast <= TOLERANCE_KM) {
            if (bestMatch == null || distanceToLast < bestMatch.distanceToRoute) {
                bestMatch = new RouteMatch(routePoints.size() - 1, 1.0, distanceToLast, cumulativeDistance);
            }
        }
        
        return bestMatch;
    }

    private PointProjection projectPointOnSegment(double[] p1, double[] p2, double lat, double lng) {
        double segmentLength = haversine(p1[0], p1[1], p2[0], p2[1]);
        
        if (segmentLength < 0.001) { // Less than 1 meter
            double distance = haversine(p1[0], p1[1], lat, lng);
            return new PointProjection(0, 0, distance);
        }
        
        double distToP1 = haversine(p1[0], p1[1], lat, lng);
        double distToP2 = haversine(p2[0], p2[1], lat, lng);
        
        // Use law of cosines for more accurate projection
        double cosAngle = (distToP1 * distToP1 + segmentLength * segmentLength - distToP2 * distToP2) 
                          / (2 * distToP1 * segmentLength);
        cosAngle = Math.max(-1, Math.min(1, cosAngle)); // Clamp to valid range
        
        double projectionLength = distToP1 * cosAngle;
        double t = Math.max(0, Math.min(1, projectionLength / segmentLength));
        
        // Interpolate position
        double projLat = p1[0] + t * (p2[0] - p1[0]);
        double projLng = p1[1] + t * (p2[1] - p1[1]);
        
        double distanceToRoute = haversine(projLat, projLng, lat, lng);
        double distanceAlongSegment = t * segmentLength;
        
        return new PointProjection(t, distanceAlongSegment, distanceToRoute);
    }

    private boolean checkViaPointsMatching(List<ViaPoints> viaPoints, double srcLat, double srcLng, double destLat, double destLng) {
        log.debug("Checking via points matching for {} via points", viaPoints.size());
        
        for (int i = 0; i < viaPoints.size(); i++) {
            ViaPoints currentVia = viaPoints.get(i);
            double pickupDist = haversine(currentVia.getPickupLatitude(), currentVia.getPickupLongitude(), srcLat, srcLng);
            
            log.debug("Via point {}: pickup distance = {:.3f}km", i, pickupDist);
            
            if (pickupDist <= TOLERANCE_KM) {
                // FIXED: Start from j = i for same via point pickup/drop, or j = i + 1 for different via points
                for (int j = i; j < viaPoints.size(); j++) {
                    ViaPoints targetVia = viaPoints.get(j);
                    double dropDist = haversine(targetVia.getDropLatitude(), targetVia.getDropLongitude(), destLat, destLng);
                    
                    log.debug("  Drop point {}: distance = {}km", j, String.format("%.3f", dropDist));
                    
                    if (dropDist <= TOLERANCE_KM) {
                        log.debug("Via points match found: pickup via {} -> drop via {}", i, j);
                        return true;
                    }
                }
            }
        }
        
        log.debug("No via points match found");
        return false;
    }

    private List<double[]> decodePolyline(String encoded) {
        List<double[]> poly = new java.util.ArrayList<>();
        
        if (encoded == null || encoded.trim().isEmpty()) {
            log.debug("Empty polyline provided");
            return poly;
        }
        
        try {
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    if (index >= len) break;
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20 && index < len);
                
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    if (index >= len) break;
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20 && index < len);
                
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                double latitude = lat / 1E5;
                double longitude = lng / 1E5;
                
                if (isValidCoordinates(latitude, longitude)) {
                    poly.add(new double[]{latitude, longitude});
                }
            }
            
            // FIXED: Add validation for point density
            if (poly.size() >= 2) {
                double totalDistance = 0;
                for (int i = 0; i < poly.size() - 1; i++) {
                    totalDistance += haversine(poly.get(i)[0], poly.get(i)[1], 
                                             poly.get(i+1)[0], poly.get(i+1)[1]);
                }
                double avgSegmentLength = totalDistance / (poly.size() - 1);
                
                if (avgSegmentLength > 5.0) { // If segments are > 5km apart
                    log.warn("Polyline points are sparse. Avg segment: {:.2f}km, Total points: {}", 
                        avgSegmentLength, poly.size());
                }
                
                log.debug("Decoded polyline: {} points, total distance: {:.2f}km, avg segment: {:.2f}km", 
                    poly.size(), totalDistance, avgSegmentLength);
            }
            
        } catch (Exception e) {
            log.error("Error decoding polyline: ", e);
            return new java.util.ArrayList<>();
        }
        
        return poly;
    }

    private CreateRide mapToEntity(RideDTO dto) {
        List<ViaPoints> viaPoints = null;
        
        if (dto.getViaPoints() != null && !dto.getViaPoints().isEmpty()) {
            viaPoints = dto.getViaPoints().stream()
                .map(v -> ViaPoints.builder()
                        .pickupLocation(v.getPickupLocation())
                        .pickupLatitude(v.getPickupLatitude())
                        .pickupLongitude(v.getPickupLongitude())
                        .dropLocation(v.getDropLocation())
                        .dropLatitude(v.getDropLatitude())
                        .dropLongitude(v.getDropLongitude())
                        .build())
                .collect(Collectors.toList());
        }

        return CreateRide.builder()
                .startPoint(dto.getStartPoint())
                .startLatitude(dto.getStartLatitude())
                .startLongitude(dto.getStartLongitude())
                .destinationPoint(dto.getDestinationPoint())
                .destinationLatitude(dto.getDestinationLatitude())
                .destinationLongitude(dto.getDestinationLongitude())
                .rideDate(dto.getRideDate())
                .rideStatus(dto.getRideStatus())
                .vehicleId(dto.getVehicleId())
                .vehicleType(dto.getVehicleType())
                .rideTime(dto.getRideTime())
                .rideStatus(RideStatus.open)
                .availableSeats(dto.getAvailableSeats())
                .viaPoints(viaPoints)
                .build();
    }

    // FIXED: New method for calculating expanded bounding box
    private BoundingBox calculateExpandedBoundingBox(double srcLat, double srcLng, double destLat, double destLng) {
        // Calculate the route distance to determine appropriate search radius
        double routeDistance = haversine(srcLat, srcLng, destLat, destLng);
        
        // Use larger search radius: minimum of SEARCH_RADIUS_KM or route distance + buffer
        double searchRadius = Math.max(SEARCH_RADIUS_KM, routeDistance + 20.0); // 20km buffer
        
        // Find center point of search area
        double centerLat = (srcLat + destLat) / 2;
        double centerLng = (srcLng + destLng) / 2;
        
        // Calculate bounding box around center point
        double latOffset = searchRadius / 111.0; // ~111km per degree latitude
        double avgLat = centerLat;
        double lngOffset = searchRadius / (111.0 * Math.cos(Math.toRadians(avgLat)));
        
        double minLat = centerLat - latOffset;
        double maxLat = centerLat + latOffset;
        double minLng = centerLng - lngOffset;
        double maxLng = centerLng + lngOffset;
        
        log.debug("Expanded bounding box calculation: route distance={:.2f}km, search radius={:.2f}km", 
            routeDistance, searchRadius);
        
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }

    // DEPRECATED: Keep original method for reference, but not used
    private BoundingBox calculateBoundingBox(double lat1, double lng1, double lat2, double lng2) {
        double minLat = Math.min(lat1, lat2) - (SEARCH_RADIUS_KM / 111.0);
        double maxLat = Math.max(lat1, lat2) + (SEARCH_RADIUS_KM / 111.0);
        double avgLat = (minLat + maxLat) / 2;
        double lngOffset = SEARCH_RADIUS_KM / (111.0 * Math.cos(Math.toRadians(avgLat)));
        double minLng = Math.min(lng1, lng2) - lngOffset;
        double maxLng = Math.max(lng1, lng2) + lngOffset;
        
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }

    // ADDED: Method for calculating bounding box around existing ride (for future use)
    private BoundingBox calculateBoundingBoxForRide(CreateRide ride, double searchRadius) {
        // Include ride's start, end, AND all via points in bounding box
        double minLat = Math.min(ride.getStartLatitude(), ride.getDestinationLatitude());
        double maxLat = Math.max(ride.getStartLatitude(), ride.getDestinationLatitude());
        double minLng = Math.min(ride.getStartLongitude(), ride.getDestinationLongitude());
        double maxLng = Math.max(ride.getStartLongitude(), ride.getDestinationLongitude());
        
        // Include via points
        if (ride.getViaPoints() != null) {
            for (ViaPoints via : ride.getViaPoints()) {
                minLat = Math.min(minLat, Math.min(via.getPickupLatitude(), via.getDropLatitude()));
                maxLat = Math.max(maxLat, Math.max(via.getPickupLatitude(), via.getDropLatitude()));
                minLng = Math.min(minLng, Math.min(via.getPickupLongitude(), via.getDropLongitude()));
                maxLng = Math.max(maxLng, Math.max(via.getPickupLongitude(), via.getDropLongitude()));
            }
        }
        
        // Add search radius
        double latOffset = searchRadius / 111.0;
        double avgLat = (minLat + maxLat) / 2;
        double lngOffset = searchRadius / (111.0 * Math.cos(Math.toRadians(avgLat)));
        
        return new BoundingBox(minLat - latOffset, maxLat + latOffset, 
                              minLng - lngOffset, maxLng + lngOffset);
    }

    private boolean isValidCoordinates(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private boolean isClose(double lat1, double lon1, double lat2, double lon2, double toleranceKm) {
        return haversine(lat1, lon1, lat2, lon2) <= toleranceKm;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private <T> ResponseModel<T> createErrorResponse(ResponseModel<T> response, HttpStatus status, String message) {
        response.setStatusCode(String.valueOf(status.value()));
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    // Inner classes
    private static class RouteMatch {
        final int pointIndex;
        final double segmentRatio;
        final double distanceToRoute;
        final double routeDistance;
        
        RouteMatch(int pointIndex, double segmentRatio, double distanceToRoute, double routeDistance) {
            this.pointIndex = pointIndex;
            this.segmentRatio = segmentRatio;
            this.distanceToRoute = distanceToRoute;
            this.routeDistance = routeDistance;
        }
    }

    private static class PointProjection {
        final double t;
        final double distanceAlongSegment;
        final double distance;
        
        PointProjection(double t, double distanceAlongSegment, double distance) {
            this.t = t;
            this.distanceAlongSegment = distanceAlongSegment;
            this.distance = distance;
        }
    }

    private static class BoundingBox {
        final double minLat, maxLat, minLng, maxLng;
        
        BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }
        
        @Override
        public String toString() {
            return String.format("BoundingBox[(%f,%f) to (%f,%f)]", minLat, minLng, maxLat, maxLng);
        }
    }
    
    
    @Override
    public ResponseModel<RideDetailsProjection> getRideDetails(UUID rideId) {
        log.info("Fetching ride details for ID: {}", rideId);
        ResponseModel<RideDetailsProjection> response = new ResponseModel<>();

        try {
            var rideDetailsOpt = rideRepository.findRideDetailsById(rideId);
            if (rideDetailsOpt.isEmpty()) {
                response.setStatusCode(String.valueOf(HttpStatus.NOT_FOUND.value()));
                response.setMessage("Ride not found for ID: " + rideId);
                return response;
            }

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setMessage("Ride details fetched successfully");
            response.setData(rideDetailsOpt.get());
        } catch (Exception e) { 
            log.error("Error fetching ride details: ", e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage("Failed to fetch ride details: " + e.getMessage());
        }

        return response;
    }
    
    @Override
    public ResponseModel<List<CreateRideProjection>> getRidesByUser(UUID userId) {
        log.info("Fetching rides created by user ID: {}", userId);
        ResponseModel<List<CreateRideProjection>> response = new ResponseModel<>();

        try {
            var rides = rideRepository.findRidesByCreatedBy(userId);
            if (rides.isEmpty()) {
                response.setStatusCode(String.valueOf(HttpStatus.NOT_FOUND.value()));
                response.setMessage("No rides found for user ID: " + userId);
                return response;
            }

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setMessage("Rides fetched successfully");
            response.setData(rides);
        } catch (Exception e) {
            log.error("Error fetching rides by user: ", e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage("Failed to fetch rides: " + e.getMessage());
        }

        return response;
    }

    @Override
    public ResponseModel<CreateRide> updateStatus(RideUpdateDto rideUpdate) {
        ResponseModel<CreateRide> response = new ResponseModel<>();

        try {
            CreateRide createRide = rideRepository.findById(rideUpdate.getId())
                    .orElseThrow(() -> new RecordNotFoundException("Ride ID not found to update the status"));

            createRide.setRideStatus(rideUpdate.getRideStatus());
            createRide.setAvailableSeats(rideUpdate.getAvailableSeats());

            CreateRide savedRide = rideRepository.save(createRide);

            response.setData(savedRide);
            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("Ride updated successfully");

        } catch (RecordNotFoundException e) {
            log.error("Ride not found: ", e);
            response.setStatusCode(HttpStatus.NOT_FOUND.toString());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating ride: ", e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            response.setMessage("Failed to update ride: " + e.getMessage());
        }

        return response;
    }

}