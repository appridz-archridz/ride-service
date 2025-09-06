package com.gorap.rideservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
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

@Service
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
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return new JwtResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getPhoneNumber(),
                userPrincipal.getAddress(),
                userPrincipal.getUserRole()
        );
    }

    /**
     * Register new user
     */
    @Override
    public UserResponse registerUser(SignupRequest signUpRequest) {
     
        if (userRepository.existsByUserName(signUpRequest.getUserName())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        if (userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        // Create new user
        User user = new User();
        user.setUserName(signUpRequest.getUserName());
        user.setEmail(signUpRequest.getEmail());
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setAddress(signUpRequest.getAddress());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        // Assign role (default USER if not provided)
        Role role = signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.USER;
        user.setRole(role);

        // Save to DB
        User savedUser = userRepository.save(user);

        // Return response
        return new UserResponse(
                savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedUser.getAddress(),
                savedUser.getRole()
        );
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
