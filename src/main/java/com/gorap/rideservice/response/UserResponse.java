package com.gorap.rideservice.response;

import java.util.UUID;

import com.gorap.rideservice.constants.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {

	private UUID id;
	private String userName;
	private String email;
	private String phoneNumber;
	private String address;
	private Role role;

	public UserResponse(UUID id, String userName, String email, String phoneNumber, String address, Role role) {
		this.id = id;
		this.userName = userName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.address = address;
		this.role = role;
	}
}
