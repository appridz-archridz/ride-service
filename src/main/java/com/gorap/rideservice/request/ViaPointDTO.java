package com.gorap.rideservice.request;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViaPointDTO {

    private UUID id;

    private String pickupLocation;
    private Double pickupLatitude;
    private Double pickupLongitude;

    private String dropLocation;
    private Double dropLatitude;
    private Double dropLongitude;
}
