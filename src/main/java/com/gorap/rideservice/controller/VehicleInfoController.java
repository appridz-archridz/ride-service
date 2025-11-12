package com.gorap.rideservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gorap.rideservice.entity.VehicleInfo;
import com.gorap.rideservice.service.VehicleInfoService;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/vehicle")
@RequiredArgsConstructor
public class VehicleInfoController {

    private final VehicleInfoService vehicleInfoService;

    @PostMapping("/create/{userId}")
    public ResponseEntity<ResponseModel<VehicleInfo>> createVehicle(
            @PathVariable UUID userId,
            @RequestBody VehicleInfo vehicleInfo) {

        log.info("Begin VehicleInfoController -> createVehicle()");

        ResponseModel<VehicleInfo> response = vehicleInfoService.createVehicle(userId, vehicleInfo);

        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(response.getStatusCode()));

        log.info("End VehicleInfoController -> createVehicle()");
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ResponseModel<VehicleInfo>> getVehicle(@PathVariable UUID vehicleId) {

        log.info("Begin VehicleInfoController -> getVehicle()");

        ResponseModel<VehicleInfo> response = vehicleInfoService.getVehicle(vehicleId);
        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(response.getStatusCode()));

        return ResponseEntity.status(status).body(response);
    }

    @PutMapping("/update/{vehicleId}")
    public ResponseEntity<ResponseModel<VehicleInfo>> updateVehicle(
            @PathVariable UUID vehicleId,
            @RequestBody VehicleInfo vehicleInfo) {

        log.info("Begin VehicleInfoController -> updateVehicle()");

        ResponseModel<VehicleInfo> response = vehicleInfoService.updateVehicle(vehicleId, vehicleInfo);
        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(response.getStatusCode()));

        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/delete/{vehicleId}")
    public ResponseEntity<ResponseModel<String>> deleteVehicle(
            @PathVariable UUID vehicleId) {

        log.info("Begin VehicleInfoController -> deleteVehicle()");

        ResponseModel<String> response = vehicleInfoService.deleteVehicle(vehicleId);
        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(response.getStatusCode()));

        return ResponseEntity.status(status).body(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseModel<List<VehicleInfo>>> getAllVehiclesByUser(
            @PathVariable UUID userId) {

        log.info("Begin VehicleInfoController -> getAllVehiclesByUser()");

        ResponseModel<List<VehicleInfo>> response = vehicleInfoService.getAllVehiclesByUser(userId);

        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(response.getStatusCode()));

        return ResponseEntity.status(status).body(response);
    }

}
