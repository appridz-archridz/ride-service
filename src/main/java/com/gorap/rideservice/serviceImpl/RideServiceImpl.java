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
import com.gorap.rideservice.service.RideService;
import com.gorap.rideservice.util.ResponseModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;

    @Override
    @Transactional
    public ResponseModel<CreateRide> createRide(RideDTO rideDTO, UUID userId) {
        log.info("Begin RideServiceImpl -> createRide()");
        ResponseModel<CreateRide> response = new ResponseModel<>();
        try {
            CreateRide ride = mapToEntity(rideDTO);
            CreateRide saved = rideRepository.save(ride);

            response.setStatusCode(HttpStatus.CREATED.toString());
            response.setMessage("Ride created successfully.");
            response.setData(saved);

            log.info("End RideServiceImpl -> createRide()");
        } catch (Exception e) {
            log.error("Error in createRide: {}", e.getMessage());
            response.setStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            response.setMessage(e.getMessage());
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
                .collect(Collectors.toList()) // Java 8 compatible
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

    // Map CreateRide entity -> RideDTO
//    private RideDTO mapToDTO(CreateRide entity) {
//        List<ViaPointDTO> viaPointDTOs = entity.getViaPoints() != null ?
//            entity.getViaPoints().stream()
//                .map(v -> ViaPointDTO.builder()
//                        .id(v.getId())
//                        .pickupLocation(v.getPickupLocation())
//                        .pickupLatitude(v.getPickupLatitude())
//                        .pickupLongitude(v.getPickupLongitude())
//                        .dropLocation(v.getDropLocation())
//                        .dropLatitude(v.getDropLatitude())
//                        .dropLongitude(v.getDropLongitude())
//                        .build())
//                .collect(Collectors.toList()) // Java 8 compatible
//            : null;
//
//        return RideDTO.builder()
//                .id(entity.getId())
//                .startPoint(entity.getStartPoint())
//                .startLatitude(entity.getStartLatitude())
//                .startLongitude(entity.getStartLongitude())
//                .destinationPoint(entity.getDestinationPoint())
//                .destinationLatitude(entity.getDestinationLatitude())
//                .destinationLongitude(entity.getDestinationLongitude())
//                .rideDate(entity.getRideDate())
//                .rideTime(entity.getRideTime())
//                .availableSeats(entity.getAvailableSeats())
//                .viaPoints(viaPointDTOs)
//                .build();
//    }
}
