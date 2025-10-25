package com.gorap.rideservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gorap.rideservice.entity.CreateRide;
import com.gorap.rideservice.repository.RideRepository;
import com.gorap.rideservice.request.RideDTO;
import com.gorap.rideservice.request.SearchRideDTO;
import com.gorap.rideservice.service.RideService;
import com.gorap.rideservice.util.HttpStatusCode;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
@Slf4j
public class RideController {

    private final RideService rideService;
    private final RideRepository rideRepository;
    private final HttpStatusCode httpStatusCode;

    @PostMapping("/create/{userId}")
    public ResponseEntity<ResponseModel<CreateRide>> createRide(
            @PathVariable UUID userId,
            @RequestBody RideDTO rideDTO) {

        log.info("Begin RideController -> createRide()");
        ResponseModel<CreateRide> response = rideService.createRide(rideDTO, userId);
        log.info("End RideController -> createRide()");
        HttpStatus httpStatus = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    @PostMapping("/search")
    public ResponseEntity<ResponseModel<List<CreateRide>>> searchRides(@RequestBody SearchRideDTO searchRideDTO) {
    	   log.info("Begin RideController -> createRide()");
           ResponseModel<List<CreateRide>> response = rideService.searchRides(searchRideDTO);
           log.info("End RideController -> createRide()");
           HttpStatus httpStatus = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
           return ResponseEntity.status(httpStatus).body(response);
    }
    
    
    @GetMapping("/debug/{rideId}")
    public ResponseEntity<?> debugRide(@PathVariable String rideId) {
        CreateRide ride = rideRepository.findById(UUID.fromString(rideId)).orElse(null);
        if (ride == null) return ResponseEntity.notFound().build();
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("id", ride.getId());
        debug.put("polylineLength", ride.getPolyline() != null ? ride.getPolyline().length() : "NULL");
        debug.put("polylineSample", ride.getPolyline() != null ? ride.getPolyline().substring(0, Math.min(100, ride.getPolyline().length())) : "NULL");
        debug.put("distanceKm", ride.getDistanceKm());
        
        return ResponseEntity.ok(debug);
    }
    
    
}
