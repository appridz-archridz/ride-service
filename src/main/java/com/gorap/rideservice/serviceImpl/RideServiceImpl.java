package com.gorap.rideservice.serviceImpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gorap.rideservice.entity.CreateRide;
import com.gorap.rideservice.entity.ViaPoints;
import com.gorap.rideservice.repository.RideRepository;
import com.gorap.rideservice.request.RideDTO;
import com.gorap.rideservice.request.SearchRideDTO;
import com.gorap.rideservice.service.RideService;
import com.gorap.rideservice.util.ResponseModel;
import com.gorap.rideservice.util.RoutingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RoutingService routingService;

    // Configuration constants
    private static final double TOLERANCE_KM = 2.0; // Distance tolerance for route matching
    private static final double STRICT_TOLERANCE_KM = 0.5; // For exact start/end matching
    private static final double MAX_DETOUR_RATIO = 3.0; // Maximum allowed detour ratio
    private static final double MIN_TRIP_DISTANCE_KM = 0.5; // Minimum viable trip distance
    private static final double SEARCH_RADIUS_KM = 50.0; // Search bounding box radius
    private static final int EARTH_RADIUS_KM = 6371; // Earth radius for Haversine formula

    @Override
    @Transactional
    public ResponseModel<CreateRide> createRide(RideDTO rideDTO, UUID userId) {
        log.info("Creating ride for user: {}", userId);
        ResponseModel<CreateRide> response = new ResponseModel<>();
        
        try {
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

            // Get route from OSRM
            RoutingService.RouteResult routeResult = routingService.getRoute(
                rideDTO.getStartLatitude(), rideDTO.getStartLongitude(),
                rideDTO.getDestinationLatitude(), rideDTO.getDestinationLongitude()
            );

            if (routeResult == null || routeResult.getPolyline() == null || routeResult.getPolyline().trim().isEmpty()) {
                log.warn("No route found from OSRM for coordinates: ({},{}) to ({},{})", 
                    rideDTO.getStartLatitude(), rideDTO.getStartLongitude(),
                    rideDTO.getDestinationLatitude(), rideDTO.getDestinationLongitude());
            }

            // Create and save ride
            CreateRide ride = mapToEntity(rideDTO);
            ride.setCreatedBy(userId);
            ride.setPolyline(routeResult != null ? routeResult.getPolyline() : null);
            ride.setDistanceKm(routeResult != null ? routeResult.getDistanceKm() : tripDistance);

            CreateRide saved = rideRepository.save(ride);

            response.setStatusCode(String.valueOf(HttpStatus.CREATED.value()));
            response.setMessage("Ride created successfully");
            response.setData(saved);
            
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

            // FIXED: Calculate expanded bounding box to include all potential rides
            BoundingBox bbox = calculateExpandedBoundingBox(
                searchRideDTO.getSourceLatitude(), searchRideDTO.getSourceLongitude(),
                searchRideDTO.getDestinationLatitude(), searchRideDTO.getDestinationLongitude()
            );

            log.debug("Search bounding box: ({:.4f},{:.4f}) to ({:.4f},{:.4f})", 
                bbox.minLat, bbox.minLng, bbox.maxLat, bbox.maxLng);

            // Get candidate rides from database
            List<CreateRide> candidateRides = rideRepository.findRidesInBoundingBox(
                bbox.minLat, bbox.maxLat, bbox.minLng, bbox.maxLng,
                searchRideDTO.getLocalDate()
            );

            log.debug("Found {} candidate rides in bounding box", candidateRides.size());

            // Filter for exact matches with detailed logging
            List<CreateRide> matched = candidateRides.stream()
                .filter(ride -> {
                    boolean isMatch = isRideMatching(ride, 
                        searchRideDTO.getSourceLatitude(), searchRideDTO.getSourceLongitude(),
                        searchRideDTO.getDestinationLatitude(), searchRideDTO.getDestinationLongitude());
                    
                    log.debug("Ride {} match result: {}", ride.getId(), isMatch);
                    return isMatch;
                })
                .collect(Collectors.toList());

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setMessage(String.format("Found %d matching rides", matched.size()));
            response.setData(matched);
            
            log.info("Search completed: {} rides found out of {} candidates", matched.size(), candidateRides.size());
            
        } catch (Exception e) {
            log.error("Error searching rides: ", e);
            return createErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to search rides: " + e.getMessage());
        }
        
        return response;
    }

    private boolean isRideMatching(CreateRide ride, double srcLat, double srcLng, double destLat, double destLng) {
        log.debug("Checking ride {} for matching", ride.getId());
        
        // Check exact route match first
        if (isExactRouteMatch(ride, srcLat, srcLng, destLat, destLng)) {
            log.debug("Exact route match for ride {}", ride.getId());
            return true;
        }

        // Check via points match
        if (ride.getViaPoints() != null && !ride.getViaPoints().isEmpty()) {
            if (checkViaPointsMatching(ride.getViaPoints(), srcLat, srcLng, destLat, destLng)) {
                log.debug("Via points match for ride {}", ride.getId());
                return true;
            }
        }

        // Check polyline match for intermediate routes
        if (ride.getPolyline() != null && !ride.getPolyline().trim().isEmpty()) {
            if (isIntermediateRouteMatching(ride, srcLat, srcLng, destLat, destLng)) {
                log.debug("Intermediate route match for ride {}", ride.getId());
                return true;
            }
        } else {
            log.debug("No polyline data for ride {}", ride.getId());
        }

        log.debug("No match found for ride {}", ride.getId());
        return false;
    }

    private boolean isExactRouteMatch(CreateRide ride, double srcLat, double srcLng, double destLat, double destLng) {
        boolean startMatch = isClose(ride.getStartLatitude(), ride.getStartLongitude(), srcLat, srcLng, STRICT_TOLERANCE_KM);
        boolean endMatch = isClose(ride.getDestinationLatitude(), ride.getDestinationLongitude(), destLat, destLng, STRICT_TOLERANCE_KM);
        
        log.debug("Exact route check for ride {}: start={}, end={}", ride.getId(), startMatch, endMatch);
        
        return startMatch && endMatch;
    }

    private boolean isIntermediateRouteMatching(CreateRide ride, double srcLat, double srcLng, double destLat, double destLng) {
        if (ride.getPolyline() == null || ride.getPolyline().trim().isEmpty()) {
            log.debug("No polyline data for ride {}", ride.getId());
            return false;
        }

        try {
            List<double[]> routePoints = decodePolyline(ride.getPolyline());
            if (routePoints.size() < 2) {
                log.debug("Insufficient polyline points for ride {}: {}", ride.getId(), routePoints.size());
                return false;
            }

            log.debug("Decoded {} polyline points for ride {}", routePoints.size(), ride.getId());
            
            // DEBUG: Log first, middle, and last points to understand route coverage
            if (routePoints.size() > 0) {
                double[] first = routePoints.get(0);
                double[] last = routePoints.get(routePoints.size() - 1);
                double[] middle = routePoints.get(routePoints.size() / 2);
                log.debug("Route sample points - First: ({:.4f},{:.4f}), Middle: ({:.4f},{:.4f}), Last: ({:.4f},{:.4f})", 
                    first[0], first[1], middle[0], middle[1], last[0], last[1]);
            }

            // Find closest points on route
            RouteMatch sourceMatch = findClosestPointOnRoute(routePoints, srcLat, srcLng);
            RouteMatch destMatch = findClosestPointOnRoute(routePoints, destLat, destLng);

            log.debug("Route matching for ride {}: source={}, dest={}", 
                ride.getId(), 
                sourceMatch != null ? String.format("dist=%.3f", sourceMatch.distanceToRoute) : "null",
                destMatch != null ? String.format("dist=%.3f", destMatch.distanceToRoute) : "null");

            // Validate matches
            if (sourceMatch == null || sourceMatch.distanceToRoute > TOLERANCE_KM ||
                destMatch == null || destMatch.distanceToRoute > TOLERANCE_KM) {
                log.debug("Route points too far from polyline for ride {}", ride.getId());
                return false;
            }

            // Check direction (source before destination)
            if (sourceMatch.routeDistance >= destMatch.routeDistance) {
                log.debug("Route direction incorrect for ride {} (source: {:.2f}, dest: {:.2f})", 
                    ride.getId(), sourceMatch.routeDistance, destMatch.routeDistance);
                return false;
            }

            // Validate segment distance
            double segmentDistance = destMatch.routeDistance - sourceMatch.routeDistance;
            double directDistance = haversine(srcLat, srcLng, destLat, destLng);

            if (segmentDistance < MIN_TRIP_DISTANCE_KM) {
                log.debug("Segment too short for ride {}: {:.2f}km", ride.getId(), segmentDistance);
                return false;
            }

            if (segmentDistance > directDistance * MAX_DETOUR_RATIO) {
                log.debug("Segment too long for ride {} (segment: {:.2f}km, direct: {:.2f}km, ratio: {:.2f})", 
                    ride.getId(), segmentDistance, directDistance, segmentDistance / directDistance);
                return false;
            }

            log.debug("Polyline match found for ride {} - Segment: {:.2f}km, Direct: {:.2f}km", 
                ride.getId(), segmentDistance, directDistance);
            return true;

        } catch (Exception e) {
            log.error("Error in route matching for ride {}: {}", ride.getId(), e.getMessage(), e);
            return false;
        }
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
                .rideTime(dto.getRideTime())
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
}