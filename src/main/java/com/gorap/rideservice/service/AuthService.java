package com.gorap.rideservice.service;

import java.util.UUID;

import com.gorap.rideservice.auth.UserPrincipal;
import com.gorap.rideservice.entity.User;
import com.gorap.rideservice.request.LoginRequest;
import com.gorap.rideservice.request.SignupRequest;
import com.gorap.rideservice.request.TokenRefreshRequest;
import com.gorap.rideservice.request.TokenRefreshResponse;
import com.gorap.rideservice.response.JwtResponse;
import com.gorap.rideservice.response.UserResponse;
import com.gorap.rideservice.util.ResponseModel;

public interface AuthService {

	ResponseModel<JwtResponse> authenticateUser( LoginRequest loginRequest);

	ResponseModel<UserResponse> registerUser( SignupRequest signUpRequest);

	String generateTokenForUser(UserPrincipal currentUser);

	boolean existsByPhoneNumber(String phoneNumber);

	boolean existsByEmail(String email);
	
	ResponseModel<SignupRequest> getProfileDetails(UUID userId);
	
	ResponseModel<User> updateProfile(User user);

	void forgotPassword(String email);

	ResponseModel<String> verifyOtp(String email, String otp);

	ResponseModel<String> updatePassword(String email, String newPassword);

	ResponseModel<TokenRefreshResponse> refreshToken(TokenRefreshRequest request);

	ResponseModel<String> logoutAllDevices(UUID id);
	
}
