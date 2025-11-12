package com.gorap.rideservice.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.gorap.rideservice.entity.VehicleInfo;
import com.gorap.rideservice.repository.VehicleInfoRepository;
import com.gorap.rideservice.service.VehicleInfoService;
import com.gorap.rideservice.util.ResponseModel;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleInfoServiceImpl implements VehicleInfoService {

    private final VehicleInfoRepository vehicleInfoRepository;

    private <T> ResponseModel<T> createError(ResponseModel<T> response, HttpStatus status, String message) {
        response.setStatusCode(String.valueOf(status.value()));
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    @Override
    @Transactional
    public ResponseModel<VehicleInfo> createVehicle(UUID userId, VehicleInfo vehicleInfo) {
        log.info("Begin VehicleInfoService -> createVehicle() for user: {}", userId);

        ResponseModel<VehicleInfo> response = new ResponseModel<>();

        try {
            // Mandatory validations
            if (vehicleInfo.getVehicleNumber() == null || vehicleInfo.getVehicleNumber().isBlank()) {
                return createError(response, HttpStatus.BAD_REQUEST, "Vehicle number is required");
            }

            if (vehicleInfo.getDlNumber() == null || vehicleInfo.getDlNumber().isBlank()) {
                return createError(response, HttpStatus.BAD_REQUEST, "DL Number is required");
            }

            if (vehicleInfo.getDlExpiry() == null) {
                return createError(response, HttpStatus.BAD_REQUEST, "DL expiry date is required");
            }

            // Set creator
            vehicleInfo.setCreatedBy(userId);

            VehicleInfo saved = vehicleInfoRepository.save(vehicleInfo);

            response.setStatusCode(String.valueOf(HttpStatus.CREATED.value()));
            response.setSuccess(true);
            response.setMessage("Vehicle info saved successfully");
            response.setData(saved);

            log.info("Vehicle info saved successfully with ID: {}", saved.getId());

        } catch (Exception e) {
            log.error("Error saving vehicle info", e);
            return createError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save vehicle info: " + e.getMessage());
        }

        return response;
    }

    @Override
    public ResponseModel<VehicleInfo> getVehicle(UUID vehicleId) {
        log.info("Fetching vehicle info for ID: {}", vehicleId);

        ResponseModel<VehicleInfo> response = new ResponseModel<>();

        try {
            Optional<VehicleInfo> vehicle = vehicleInfoRepository.findById(vehicleId);

            if (vehicle.isEmpty()) {
                return createError(response, HttpStatus.NOT_FOUND, "Vehicle not found");
            }

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setSuccess(true);
            response.setMessage("Vehicle fetched successfully");
            response.setData(vehicle.get());

        } catch (Exception e) {
            log.error("Error fetching vehicle info", e);
            return createError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch vehicle info: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public ResponseModel<VehicleInfo> updateVehicle(UUID vehicleId, VehicleInfo updatedInfo) {

        log.info("Updating vehicle info for ID: {}", vehicleId);

        ResponseModel<VehicleInfo> response = new ResponseModel<>();

        try {
            Optional<VehicleInfo> existingOpt = vehicleInfoRepository.findById(vehicleId);

            if (existingOpt.isEmpty()) {
                return createError(response, HttpStatus.NOT_FOUND, "Vehicle not found");
            }

            VehicleInfo existing = existingOpt.get();

            // Update only provided optional fields
            if (updatedInfo.getVehicleNumber() != null)
                existing.setVehicleNumber(updatedInfo.getVehicleNumber());

            if (updatedInfo.getDlNumber() != null)
                existing.setDlNumber(updatedInfo.getDlNumber());

            if (updatedInfo.getDlExpiry() != null)
                existing.setDlExpiry(updatedInfo.getDlExpiry());

            existing.setInsurancePolicyNumber(updatedInfo.getInsurancePolicyNumber());
            existing.setInsuranceExpiry(updatedInfo.getInsuranceExpiry());
            existing.setIsCommercialInsurance(updatedInfo.getIsCommercialInsurance());
            existing.setPucNumber(updatedInfo.getPucNumber());
            existing.setPucExpiry(updatedInfo.getPucExpiry());
            existing.setPermitNumber(updatedInfo.getPermitNumber());
            existing.setPermitExpiry(updatedInfo.getPermitExpiry());
            existing.setIdProofNumber(updatedInfo.getIdProofNumber());
            existing.setVehicleFrontPhotoUrl(updatedInfo.getVehicleFrontPhotoUrl());
            existing.setConsentGiven(updatedInfo.isConsentGiven());

            VehicleInfo saved = vehicleInfoRepository.save(existing);

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setSuccess(true);
            response.setMessage("Vehicle updated successfully");
            response.setData(saved);

        } catch (Exception e) {
            log.error("Error updating vehicle info", e);
            return createError(response,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update vehicle info: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public ResponseModel<String> deleteVehicle(UUID vehicleId) {

        log.info("Deleting vehicle info: {}", vehicleId);

        ResponseModel<String> response = new ResponseModel<>();

        try {
            if (!vehicleInfoRepository.existsById(vehicleId)) {
                return createError(response, HttpStatus.NOT_FOUND, "Vehicle not found");
            }

            vehicleInfoRepository.deleteById(vehicleId);

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setSuccess(true);
            response.setMessage("Vehicle deleted successfully");
            response.setData("Deleted");

        } catch (Exception e) {
            log.error("Error deleting vehicle info", e);
            return createError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete vehicle: " + e.getMessage());
        }

        return response;
    }
    
    @Override
    public ResponseModel<List<VehicleInfo>> getAllVehiclesByUser(UUID userId) {

        log.info("Fetching all vehicles for user: {}", userId);

        ResponseModel<List<VehicleInfo>> response = new ResponseModel<>();

        try {
            List<VehicleInfo> vehicles = vehicleInfoRepository.findByCreatedBy(userId);

            response.setStatusCode(String.valueOf(HttpStatus.OK.value()));
            response.setSuccess(true);
            response.setMessage("Vehicles fetched successfully");
            response.setData(vehicles);

        } catch (Exception e) {
            log.error("Error fetching vehicles for user: {}", userId, e);
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setSuccess(false);
            response.setMessage("Failed to fetch vehicles: " + e.getMessage());
        }

        return response;
    }

}
