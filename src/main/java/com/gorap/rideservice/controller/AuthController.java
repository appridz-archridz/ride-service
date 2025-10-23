package com.gorap.rideservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gorap.rideservice.auth.UserPrincipal;
import com.gorap.rideservice.request.LoginRequest;
import com.gorap.rideservice.request.SignupRequest;
import com.gorap.rideservice.response.JwtResponse;
import com.gorap.rideservice.response.MessageResponse;
import com.gorap.rideservice.response.UserResponse;
import com.gorap.rideservice.service.AuthService;
import com.gorap.rideservice.util.HttpStatusCode;
import com.gorap.rideservice.util.ResponseModel;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Slf4j
public class AuthController {

	private final AuthService authService;
	private final HttpStatusCode httpStatusCode;

	@PostMapping("/signin")
	public ResponseEntity<ResponseModel<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		ResponseModel<JwtResponse> jwtResponse = authService.authenticateUser(loginRequest);
		HttpStatus httpStatusFromCode = httpStatusCode.getHttpStatusFromCode(jwtResponse.getStatusCode());
		return ResponseEntity.status(httpStatusFromCode).body(jwtResponse);
	}

	@PostMapping("/signup")
	public ResponseEntity<ResponseModel<UserResponse>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		ResponseModel<UserResponse> userResponse = authService.registerUser(signUpRequest);
		HttpStatus httpStatusFromCode = httpStatusCode.getHttpStatusFromCode(userResponse.getStatusCode());
		return ResponseEntity.status(httpStatusFromCode).body(userResponse);
	}

	@GetMapping("/check/username/{username}")
	public ResponseEntity<?> checkUsernameAvailability(@PathVariable String username) {
		boolean isAvailable = !authService.existsByEmail(username);
		return ResponseEntity.ok(
				new MessageResponse(isAvailable ? "Username is available" : "Username is already taken", isAvailable));
	}

	@GetMapping("/check/email/{email}")
	public ResponseEntity<?> checkEmailAvailability(@PathVariable String email) {
		boolean isAvailable = !authService.existsByEmail(email);
		return ResponseEntity
				.ok(new MessageResponse(isAvailable ? "Email is available" : "Email is already in use", isAvailable));
	}

	@GetMapping("/check/phone/{phoneNumber}")
	public ResponseEntity<?> checkPhoneAvailability(@PathVariable String phoneNumber) {
		boolean isAvailable = !authService.existsByPhoneNumber(phoneNumber);
		return ResponseEntity.ok(new MessageResponse(
				isAvailable ? "Phone number is available" : "Phone number is already in use", isAvailable));
	}

	@GetMapping("/profile")
	public ResponseEntity<ResponseModel<UserResponse>> getUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
	    ResponseModel<UserResponse> response = new ResponseModel<>();
	    try {
	        UserResponse userResponse = new UserResponse(
	                currentUser.getId(),
	                currentUser.getUsername(),
	                currentUser.getEmail(),
	                currentUser.getPhoneNumber(),
	                currentUser.getAddress(),
	                currentUser.getUserRole()
	        );

	        response.setData(userResponse);
	        response.setStatusCode(HttpStatus.OK.toString());
	        response.setMessage("User profile retrieved successfully");

	        return ResponseEntity.ok(response);
	    } catch (Exception e) {
	        log.error("Error retrieving user profile", e);

	        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
	        response.setMessage("Error retrieving user profile: " + e.getMessage());

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	    }
	}


	@PostMapping("/logout")
	public ResponseEntity<?> logoutUser() {
		return ResponseEntity.ok(new MessageResponse("User logged out successfully!"));
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(@AuthenticationPrincipal UserPrincipal currentUser) {
		try {
			// Generate new token for current user
			String newToken = authService.generateTokenForUser(currentUser);

			JwtResponse jwtResponse = new JwtResponse(newToken, currentUser.getId(), currentUser.getUsername(),
					currentUser.getEmail(), currentUser.getPhoneNumber(), currentUser.getAddress(),
					currentUser.getUserRole());

			return ResponseEntity.ok(jwtResponse);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new MessageResponse("Error refreshing token!", false));
		}
	}
	
}