package com.gorap.rideservice.service;

import com.gorap.rideservice.auth.UserPrincipal;
import com.gorap.rideservice.request.LoginRequest;
import com.gorap.rideservice.request.SignupRequest;
import com.gorap.rideservice.response.JwtResponse;
import com.gorap.rideservice.response.UserResponse;
import com.gorap.rideservice.util.ResponseModel;

public interface AuthService {

	ResponseModel<JwtResponse> authenticateUser( LoginRequest loginRequest);

	UserResponse registerUser( SignupRequest signUpRequest);

	String generateTokenForUser(UserPrincipal currentUser);

	boolean existsByPhoneNumber(String phoneNumber);

	boolean existsByEmail(String email);

}
