package com.gorap.rideservice.controller;


import org.springframework.beans.factory.annotation.Autowired;
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
import lombok.NoArgsConstructor;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
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
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            UserResponse userResponse = authService.registerUser(signUpRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("User registered successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error occurred during registration!", false));
        }
    }
    
  
    
    @GetMapping("/check/username/{username}")
    public ResponseEntity<?> checkUsernameAvailability(@PathVariable String username) {
        boolean isAvailable = !authService.existsByEmail(username);
        return ResponseEntity.ok(new MessageResponse(
                isAvailable ? "Username is available" : "Username is already taken",
                isAvailable
        ));
    }
    
    @GetMapping("/check/email/{email}")
    public ResponseEntity<?> checkEmailAvailability(@PathVariable String email) {
        boolean isAvailable = !authService.existsByEmail(email);
        return ResponseEntity.ok(new MessageResponse(
                isAvailable ? "Email is available" : "Email is already in use",
                isAvailable
        ));
    }
    

    @GetMapping("/check/phone/{phoneNumber}")
    public ResponseEntity<?> checkPhoneAvailability(@PathVariable String phoneNumber) {
        boolean isAvailable = !authService.existsByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(new MessageResponse(
                isAvailable ? "Phone number is available" : "Phone number is already in use",
                isAvailable
        ));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            UserResponse userResponse = new UserResponse(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getEmail(),
                    currentUser.getPhoneNumber(),
                    currentUser.getAddress(),
                    currentUser.getUserRole()
            );
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error retrieving user profile!", false));
        }
    }
    
 
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // For JWT, logout is typically handled on the client side by removing the token
        // You could implement token blacklisting here if needed
        return ResponseEntity.ok(new MessageResponse("User logged out successfully!"));
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            // Generate new token for current user
            String newToken = authService.generateTokenForUser(currentUser);
            
            JwtResponse jwtResponse = new JwtResponse(
                    newToken,
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getEmail(),
                    currentUser.getPhoneNumber(),
                    currentUser.getAddress(),
                    currentUser.getUserRole()
            );
            
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error refreshing token!", false));
        }
    }
}