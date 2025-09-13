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

    // Increased tolerance for intermediate point matching
    private static final double TOLERANCE_KM = 2.0; // 2km tolerance for better matching
    private static final double STRICT_TOLERANCE_KM = 0.5; // For exact start/end matching

    @Override
    @Transactional
    public ResponseModel<CreateRide> createRide(RideDTO rideDTO, UUID userId) {
        log.info("Begin RideServiceImpl -> createRide()");
        ResponseModel<CreateRide> response = new ResponseModel<>();
        try {
            // 🔹 Call OSRM online API for polyline + distance
            RoutingService.RouteResult routeResult = routingService.getRoute(
                rideDTO.getStartLatitude(), rideDTO.getStartLongitude(),
                rideDTO.getDestinationLatitude(), rideDTO.getDestinationLongitude()
            );

            // 🔹 Map DTO -> Entity
            CreateRide ride = mapToEntity(rideDTO);
            System.out.println("route"+routeResult.getPolyline());
            ride.setPolyline(routeResult.getPolyline());
            ride.setDistanceKm(routeResult.getDistanceKm());

            CreateRide saved = rideRepository.save(ride);

            response.setStatusCode(HttpStatus.CREATED.toString());
            response.setMessage("Ride created successfully.");
            response.setData(saved);

            log.info("End RideServiceImpl -> createRide()");
        } catch (Exception e) {
            log.error("Error in createRide: {}", e.getMessage(), e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage("Failed to create ride: " + e.getMessage());
            response.setData(null);
        }
        return response;
    }

    // Map RideDTO -> CreateRide entity
    private CreateRide mapToEntity(RideDTO dto) {
        List<ViaPoints> viaPoints = dto.getViaPoints() != null ?
            dto.getViaPoints().stream()
                .map(v -> ViaPoints.builder()
                        .pickupLocation(v.getPickupLocation())
                        .pickupLatitude(v.getPickupLatitude())
                        .pickupLongitude(v.getPickupLongitude())
                        .dropLocation(v.getDropLocation())
                        .dropLatitude(v.getDropLatitude())
                        .dropLongitude(v.getDropLongitude())
                        .build())
                .collect(Collectors.toList())
            : null;

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

    @Override
    @Transactional(readOnly = true)
    public ResponseModel<List<CreateRide>> searchRides(SearchRideDTO searchRideDTO) {
        log.info("Begin RideServiceImpl -> searchRides()");
        ResponseModel<List<CreateRide>> response = new ResponseModel<>();
        try {
            List<CreateRide> allRides = rideRepository.findAll();

            List<CreateRide> matched = allRides.stream()
                .filter(ride -> isRideMatching(
                        ride,
                        searchRideDTO.getSourceLatitude(), searchRideDTO.getSourceLongitude(),
                        searchRideDTO.getDestinationLatitude(), searchRideDTO.getDestinationLongitude()
                ))
                .collect(Collectors.toList());

            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("Rides fetched successfully.");
            response.setData(matched);

            log.info("End RideServiceImpl -> searchRides() - Found {} matching rides", matched.size());
        } catch (Exception e) {
            log.error("Error in searchRides: {}", e.getMessage(), e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage("Failed to search rides: " + e.getMessage());
            response.setData(null);
        }
        return response;
    }

    /**
     * ✅ Enhanced matching logic for intermediate routes
     */
    private boolean isRideMatching(CreateRide ride,
                                   double srcLat, double srcLng,
                                   double destLat, double destLng) {
        
        log.debug("Checking ride from {} to {} against search from ({},{}) to ({},{})", 
            ride.getStartPoint(), ride.getDestinationPoint(), srcLat, srcLng, destLat, destLng);
        
        // 1. First check if it's an exact route match (start to end)
        if (isClose(ride.getStartLatitude(), ride.getStartLongitude(), srcLat, srcLng, STRICT_TOLERANCE_KM)
                && isClose(ride.getDestinationLatitude(), ride.getDestinationLongitude(), destLat, destLng, STRICT_TOLERANCE_KM)) {
            log.debug("Exact route match found");
            return true;
        }

        // 2. Check viaPoints if available (for explicit intermediate stops)
        if (ride.getViaPoints() != null && !ride.getViaPoints().isEmpty()) {
            if (checkViaPointsMatching(ride.getViaPoints(), srcLat, srcLng, destLat, destLng)) {
                log.debug("Via points match found");
                return true;
            }
        }

        // 3. ✅ MAIN FIX: Enhanced polyline matching for intermediate routes
        if (ride.getPolyline() != null) {
            boolean polylineMatch = isIntermediateRouteMatching(ride, srcLat, srcLng, destLat, destLng);
            if (polylineMatch) {
                log.debug("Intermediate route match found via polyline");
                return true;
            }
        }

        return false;
    }

    /**
     * ✅ NEW METHOD: Enhanced intermediate route matching
     */
    private boolean isIntermediateRouteMatching(CreateRide ride, double srcLat, double srcLng, double destLat, double destLng) {
        if (ride.getPolyline() == null) return false;

        try {
            // Get the route polyline points
            List<double[]> routePoints = decodePolyline(ride.getPolyline());
            if (routePoints.isEmpty()) return false;

            // Find the closest points on the route to source and destination
            PointMatch sourceMatch = findClosestPointOnRoute(routePoints, srcLat, srcLng);
            PointMatch destMatch = findClosestPointOnRoute(routePoints, destLat, destLng);

            // Check if both points are close enough to the route
            if (sourceMatch == null || destMatch == null) {
                log.debug("Source or destination not close enough to route. Source match: {}, Dest match: {}", 
                    sourceMatch != null, destMatch != null);
                return false;
            }

            // Check if source comes before destination in the route
            if (sourceMatch.index >= destMatch.index) {
                log.debug("Source point comes after destination point in route. Source index: {}, Dest index: {}", 
                    sourceMatch.index, destMatch.index);
                return false;
            }

            // ✅ Additional validation: Check if the segment makes sense
            double segmentDistance = calculateRouteDistance(routePoints, sourceMatch.index, destMatch.index);
            double directDistance = haversine(srcLat, srcLng, destLat, destLng);
            
            // If route segment is too much longer than direct distance, it might not be a good match
            if (segmentDistance > directDistance * 3) { // Allow up to 3x detour
                log.debug("Route segment too long compared to direct distance. Segment: {}km, Direct: {}km", 
                    segmentDistance, directDistance);
                return false;
            }

            log.debug("✅ Intermediate route match: Source at index {}, Dest at index {}, Segment distance: {}km", 
                sourceMatch.index, destMatch.index, segmentDistance);
            return true;

        } catch (Exception e) {
            log.error("Error in intermediate route matching: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Helper class to store point match information
     */
    private static class PointMatch {
        int index;
        double distance;
        
        PointMatch(int index, double distance) {
            this.index = index;
            this.distance = distance;
        }
    }

    /**
     * Find the closest point on the route to given coordinates
     * Enhanced to handle any location along the route
     */
    private PointMatch findClosestPointOnRoute(List<double[]> routePoints, double lat, double lng) {
        PointMatch closest = null;
        
        // Also check interpolated points between polyline points for better accuracy
        for (int i = 0; i < routePoints.size(); i++) {
            double[] point = routePoints.get(i);
            double distance = haversine(point[0], point[1], lat, lng);
            
            if (distance <= TOLERANCE_KM) { // Within acceptable tolerance
                if (closest == null || distance < closest.distance) {
                    closest = new PointMatch(i, distance);
                }
            }
            
            // ✅ NEW: Check interpolated points between consecutive route points
            if (i < routePoints.size() - 1) {
                PointMatch interpolatedMatch = findClosestOnSegment(
                    routePoints.get(i), routePoints.get(i + 1), lat, lng, i
                );
                if (interpolatedMatch != null && 
                    (closest == null || interpolatedMatch.distance < closest.distance)) {
                    closest = interpolatedMatch;
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Find closest point on a line segment between two polyline points
     */
    private PointMatch findClosestOnSegment(double[] point1, double[] point2, 
                                          double searchLat, double searchLng, int segmentIndex) {
        // Vector from point1 to point2
        double dx = point2[1] - point1[1]; // longitude difference
        double dy = point2[0] - point1[0]; // latitude difference
        
        if (dx == 0 && dy == 0) {
            // Points are the same
            double distance = haversine(point1[0], point1[1], searchLat, searchLng);
            return distance <= TOLERANCE_KM ? new PointMatch(segmentIndex, distance) : null;
        }
        
        // Calculate projection parameter
        double t = ((searchLng - point1[1]) * dx + (searchLat - point1[0]) * dy) / (dx * dx + dy * dy);
        
        // Clamp t to [0, 1] to stay on segment
        t = Math.max(0, Math.min(1, t));
        
        // Find closest point on segment
        double closestLat = point1[0] + t * dy;
        double closestLng = point1[1] + t * dx;
        
        double distance = haversine(closestLat, closestLng, searchLat, searchLng);
        
        return distance <= TOLERANCE_KM ? 
            new PointMatch(segmentIndex + (int)(t * 1000), distance) : null; // Use fractional index
    }

    /**
     * Calculate distance along the route between two point indices
     */
    private double calculateRouteDistance(List<double[]> points, int startIndex, int endIndex) {
        double distance = 0.0;
        for (int i = startIndex; i < endIndex && i < points.size() - 1; i++) {
            distance += haversine(points.get(i)[0], points.get(i)[1], 
                                points.get(i + 1)[0], points.get(i + 1)[1]);
        }
        return distance;
    }

    /**
     * Check via points matching
     */
    private boolean checkViaPointsMatching(List<ViaPoints> viaPoints, double srcLat, double srcLng, double destLat, double destLng) {
        boolean foundSource = false;
        
        for (ViaPoints vp : viaPoints) {
            // Check if this via point matches our source
            if (!foundSource && isClose(vp.getPickupLatitude(), vp.getPickupLongitude(), srcLat, srcLng, TOLERANCE_KM)) {
                foundSource = true;
            }
            // If we found source, check if this via point matches our destination
            else if (foundSource && isClose(vp.getDropLatitude(), vp.getDropLongitude(), destLat, destLng, TOLERANCE_KM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ✅ Lightweight polyline decoder (no external library)
     */
    private List<double[]> decodePolyline(String encoded) {
        List<double[]> poly = new java.util.ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            poly.add(new double[]{latitude, longitude});
        }
        return poly;
    }

    // Overloaded method with custom tolerance
    private boolean isClose(double lat1, double lon1, double lat2, double lon2, double toleranceKm) {
        double distance = haversine(lat1, lon1, lat2, lon2);
        return distance <= toleranceKm;
    }

    // Original method with default tolerance
    private boolean isClose(double lat1, double lon1, double lat2, double lon2) {
        return isClose(lat1, lon1, lat2, lon2, TOLERANCE_KM);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}