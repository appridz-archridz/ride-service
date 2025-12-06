package com.gorap.rideservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.request.RideSearchRequest;
import com.gorap.rideservice.response.RideSearchProjection;
import com.gorap.rideservice.service.RideRequestService;
import com.gorap.rideservice.util.HttpStatusCode;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/request/rides")
@RequiredArgsConstructor
@Slf4j
public class RideRequestController {

    private final RideRequestService rideRequestService;
    private final HttpStatusCode httpStatusCode;

    @PostMapping("/create/{userId}")
    public ResponseEntity<ResponseModel<RideRequest>> createRideRequest(
            @PathVariable UUID userId,
            @RequestBody RideRequest rideRequest) {

        log.info("Begin RideRequestController -> createcreateRideRequestRide()");
        ResponseModel<RideRequest> response = rideRequestService.createRideRequest(userId, rideRequest);
        HttpStatus httpStatus = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        log.info("End RideRequestController -> createRideRequest()");
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    @PostMapping("/search")
    public ResponseEntity<ResponseModel<List<RideSearchProjection>>> searchRides(
            @RequestBody RideSearchRequest
            searchRideDTO) {

        log.info("Begin RideController -> searchRide()");
        ResponseModel<List<RideSearchProjection>> response = rideRequestService.searchRides(searchRideDTO);
        log.info("End RideController -> searchRide()");

        HttpStatus status = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        return ResponseEntity.status(status).body(response);
    }
    
    
    @PutMapping("/update/{rideId}")
    public ResponseEntity<ResponseModel<RideRequest>> updateRideRequest(
            @PathVariable UUID rideId,
            @RequestBody RideRequest rideRequest) {

        log.info("Begin RideRequestController -> updateRideRequest()");
        ResponseModel<RideRequest> response = rideRequestService.updateRideRequest(rideId, rideRequest);
        HttpStatus httpStatus = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        log.info("End RideRequestController -> updateRideRequest()");
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    
    @PatchMapping("/cancel/{rideId}")
    public ResponseEntity<ResponseModel<RideRequest>> cancelRide(
            @PathVariable UUID rideId) {

        log.info("Begin RideRequestController -> cancelRide()");
        ResponseModel<RideRequest> response = rideRequestService.cancelRide(rideId);
        HttpStatus httpStatus = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        log.info("End RideRequestController -> cancelRide()");
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    @PostMapping("/my-rides")
    public ResponseEntity<ResponseModel<List<RideSearchProjection>>> myRequestedRides(
            @RequestBody RideSearchRequest
            searchRideDTO) {

        log.info("Begin RideController -> searchRide()");
        ResponseModel<List<RideSearchProjection>> response = rideRequestService.myRequestedRides(searchRideDTO);
        log.info("End RideController -> searchRide()");

        HttpStatus status = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        return ResponseEntity.status(status).body(response);
    }
    
    
    @GetMapping("/get/{rideId}")
    public ResponseEntity<ResponseModel<RideRequest>> getRideById(
            @PathVariable UUID rideId) {

        log.info("Begin RideRequestController -> getRideById()");
        ResponseModel<RideRequest> response = rideRequestService.getRideById(rideId);
        HttpStatus httpStatus = httpStatusCode.getHttpStatusFromCode(response.getStatusCode());
        log.info("End RideRequestController -> getRideById()");
        return ResponseEntity.status(httpStatus).body(response);
    }



    
    
    
    
}
