package com.gorap.rideservice.serviceImpl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gorap.rideservice.auth.UserPrincipal;
import com.gorap.rideservice.constants.Role;
import com.gorap.rideservice.entity.User;
import com.gorap.rideservice.repository.UserRepository;
import com.gorap.rideservice.request.LoginRequest;
import com.gorap.rideservice.request.SignupRequest;
import com.gorap.rideservice.response.JwtResponse;
import com.gorap.rideservice.response.UserResponse;
import com.gorap.rideservice.service.AuthService;
import com.gorap.rideservice.util.JwtUtils;
import com.gorap.rideservice.util.ResponseModel;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Authenticate user and generate JWT token
     */
    @Override
    public ResponseModel<JwtResponse> authenticateUser(LoginRequest loginRequest) {
        ResponseModel<JwtResponse> response = new ResponseModel<>();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            JwtResponse jwtResponse = new JwtResponse(
                    jwt,
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    userPrincipal.getPhoneNumber(),
                    userPrincipal.getAddress(),
                    userPrincipal.getUserRole()
            );

            response.setData(jwtResponse);
            response.setSuccess(true);
            response.setStatusCode(HttpStatus.OK.toString());
            response.setMessage("User authenticated successfully");

        } catch (BadCredentialsException e) {
            log.error("Invalid username or password", e);
            response.setStatusCode(HttpStatus.UNAUTHORIZED.toString());
            response.setMessage("Invalid username or password");
        } catch (DisabledException e) {
            log.error("User account is disabled", e);
            response.setStatusCode(HttpStatus.FORBIDDEN.toString());
            response.setMessage("User account is disabled");
        } catch (LockedException e) {
            log.error("User account is locked", e);
            response.setStatusCode(HttpStatus.LOCKED.toString());
            response.setMessage("User account is locked");
        } catch (Exception e) {
            log.error("Authentication error", e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            response.setMessage("Authentication failed: " + e.getMessage());
        } finally {
			response.setSuccess(false);
		}
        return response;
    }


    /**
     * Register new user
     */
    @Override
    public ResponseModel<UserResponse> registerUser(SignupRequest signUpRequest) {
        ResponseModel<UserResponse> response = new ResponseModel<>();
        try {
            if (userRepository.existsByUserName(signUpRequest.getUserName())) {
                response.setStatusCode(HttpStatus.CONFLICT.toString());
                response.setMessage("Username is already taken!");
                return response;
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                response.setStatusCode(HttpStatus.CONFLICT.toString());
                response.setMessage("Email is already in use!");
                return response;
            }

            if (userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())) {
                response.setStatusCode(HttpStatus.CONFLICT.toString());
                response.setMessage("Phone number is already in use!");
                return response;
            }

            // Create new user
            User user = new User();
            user.setUserName(signUpRequest.getUserName());
            user.setEmail(signUpRequest.getEmail());
            user.setPhoneNumber(signUpRequest.getPhoneNumber());
            user.setAddress(signUpRequest.getAddress());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            user.setDeviceName(signUpRequest.getDeviceName());
            user.setProfilePic(signUpRequest.getProfilePic());

            // Assign role (default USER if not provided)
            Role role = signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.USER;
            user.setRole(role);

            // Save to DB
            User savedUser = userRepository.save(user);

            UserResponse userResponse = new UserResponse(
                    savedUser.getId(),
                    savedUser.getUserName(),
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    savedUser.getAddress(),
                    savedUser.getRole()
            );

            response.setData(userResponse);
            response.setSuccess(true);
            response.setStatusCode(HttpStatus.CREATED.toString());
            response.setMessage("User registered successfully");

        } catch (Exception e) {
            log.error("Error during user registration", e);
            response.setSuccess(false);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            response.setMessage("Failed to register user: " + e.getMessage());
        }
        return response;
    }
    
    @Override
    public ResponseModel<SignupRequest> getProfileDetails(UUID userId) {
    	try {
    		User userDetails = userRepository.getById(userId);
    	} catch (Exception e) {
    		
    	}
    	return null;
    }


    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }


    @Override
    public boolean existsByPhoneNumber(String phone) {
        return userRepository.existsByPhoneNumber(phone);
    }

  
    @Override
    public String generateTokenForUser(UserPrincipal userPrincipal) {
        return jwtUtils.generateTokenFromUsername(userPrincipal.getUsername());
    }
}
