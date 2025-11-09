package com.gorap.rideservice.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gorap.rideservice.entity.RideRequest;
import com.gorap.rideservice.service.RideRequestService;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Slf4j
public class RideRequestController {

    private final RideRequestService rideRequestService;

    @PostMapping("/create/{userId}")
    public ResponseEntity<ResponseModel<RideRequest>> createRideRequest(
            @PathVariable UUID userId,
            @RequestBody RideRequest rideRequest) {

        log.info("Begin RideRequestController -> createcreateRideRequestRide()");
        ResponseModel<RideRequest> response = rideRequestService.createRideRequest(userId, rideRequest);
        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(response.getStatusCode()));
        log.info("End RideRequestController -> createRideRequest()");
        return ResponseEntity.status(status).body(response);
    }
}
