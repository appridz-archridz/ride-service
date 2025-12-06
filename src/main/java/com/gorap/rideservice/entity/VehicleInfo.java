package com.gorap.rideservice.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

import com.gorap.rideservice.constants.VehicleType;

@Entity
@Table(name = "vehicle_info", schema = "ride")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleInfo  extends BaseEntity{
	
	@Column(name="vehicle_type")
	@Enumerated(EnumType.STRING)
	private VehicleType vehicleType;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "dl_number")
    private String dlNumber;

    @Column(name = "dl_expiry")
    private LocalDate dlExpiry;

    // Optional
    @Column(name = "insurance_policy_number", length = 30)
    private String insurancePolicyNumber;

    // Optional
    private LocalDate insuranceExpiry;

    // Optional
    private Boolean isCommercialInsurance;

    // Optional
    @Column(name = "puc_number", length = 30)
    private String pucNumber;

    // Optional
    private LocalDate pucExpiry;


    @Column(name = "permit_number", length = 40)
    private String permitNumber;

    // Optional
    private LocalDate permitExpiry;

    // Optional
    @Column(name = "id_proof_number", length = 10)
    private String idProofNumber;


    private String vehicleFrontPhotoUrl;

    // Mandatory (for legal consent)
    @Column(name = "consent_given")
    private boolean consentGiven;
}
