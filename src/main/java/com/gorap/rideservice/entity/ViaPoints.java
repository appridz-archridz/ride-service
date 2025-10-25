package com.gorap.rideservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Entity
@Table(name="via_points")
public class ViaPoints extends BaseEntity{

	private String pickupLocation;
	private Double pickupLatitude;
	private Double pickupLongitude;

	private String dropLocation;
	private Double dropLatitude;
	private Double dropLongitude;

	// getters & setters
}
