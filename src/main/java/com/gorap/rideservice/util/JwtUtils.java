package com.gorap.rideservice.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.gorap.rideservice.auth.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
    
    @Value("${app.jwtSecret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${app.jwtExpirationMs:900000}")         // 15 minutes
    private long jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs:604800000}") // 7 days
    private long refreshTokenExpirationMs;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // userName from your entity
                .claim("userId", userPrincipal.getId().toString()) // Add UUID as claim
                .claim("email", userPrincipal.getEmail()) // Add email as claim
                .claim("role", userPrincipal.getUserRole().name()) // Add role as claim
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(jwtExpirationMs, ChronoUnit.MILLIS)))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(jwtExpirationMs, ChronoUnit.MILLIS)))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public String generateRefreshTokenFromEmail(String email, UUID userId) {
        return Jwts.builder()
                .subject(email) // ✅ EMAIL as main identifier
                .claim("userId", userId.toString())
                .claim("type", "REFRESH")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS)))
                .signWith(getSigningKey())
                .compact();
    }
    
    
    // New method to get UUID from token
    public UUID getUserIdFromJwtToken(String token) {
        String userIdStr = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", String.class);
        return UUID.fromString(userIdStr);
    }
    
    // New method to get email from token
    public String getEmailFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }
    
    // New method to get role from token
    public String getRoleFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
    
    public boolean validateJwtToken(String authToken) {
    	System.out.println("auth");
        try {
        	Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken)
                    .getPayload();
            return true;
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        } catch (JwtException e) {
            System.err.println("JWT error: " + e.getMessage());
        }
        
        return false;
    }
    public String generateTokenFromEmail(String email) {
        return Jwts.builder()
                .subject(email) 
                .claim("type", "ACCESS")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(jwtExpirationMs, ChronoUnit.MILLIS)))
                .signWith(getSigningKey())
                .compact();
    }
    
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}