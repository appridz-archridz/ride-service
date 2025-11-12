package com.gorap.rideservice.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TokenRefreshResponse DTO
 * 
 * PURPOSE: Response body when refresh token is successful
 * 
 * ENDPOINT: POST /api/auth/refresh-token
 * 
 * SUCCESS RESPONSE:
 * {
 *   "accessToken": "eyJhbGci... (NEW 15-minute token)",
 *   "refreshToken": "eyJzdWI... (NEW 7-day token or same one)",
 *   "tokenType": "Bearer"
 * }
 * 
 * WHAT USER DOES:
 * 1. Store new accessToken (replace old expired one)
 * 2. Store new refreshToken (if token rotation enabled)
 * 3. Continue using app without re-login!
 */
@Data  // ✅ Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // ✅ Lombok: Generates no-args constructor
@AllArgsConstructor  // ✅ Lombok: Generates all-args constructor
public class TokenRefreshResponse {
    
    /**
     * New access token (short-lived, 15 minutes)
     * 
     * EXAMPLE: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXNlcklkIjoiMTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc0MDAwIiwiZW1haWwiOiJqb2huQGV4YW1wbGUuY29tIiwicm9sZSI6IlVTRVIiLCJ0eXBlIjoiQUNDRVNTIiwiaWF0IjoxNjk5ODc2NTQzLCJleHAiOjE2OTk4Nzc0NDN9.aB3dF6gH9jK2mN5pQ8rS1tV4wX7yZ0cE3fG6hJ9kL2nM5"
     * 
     * PURPOSE: Access protected endpoints
     * EXPIRES: 15 minutes
     * USAGE: Authorization: Bearer {accessToken}
     */
    private String accessToken;
    
    /**
     * New refresh token (long-lived, 7 days)
     * 
     * EXAMPLE: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXNlcklkIjoiMTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc0MDAwIiwidHlwZSI6IlJFRlJFU0giLCJpYXQiOjE2OTk4NzY1NDMsImV4cCI6MTcwMDQ4MTM0M30.K7x2mP9qR4tY6wZ8vC1nM5bX3hG7jL0sA4fD9kE2pQ6r"
     * 
     * PURPOSE: Get new access tokens
     * EXPIRES: 7 days
     * 
     * TOKEN ROTATION:
     * - Option 1: Return SAME refresh token (simple)
     * - Option 2: Return NEW refresh token, revoke old one (more secure) ✅
     * 
     * WHY NEW TOKEN:
     * - If refresh token is stolen, attacker has limited time
     * - Old token becomes invalid immediately
     * - More secure but requires frontend to update stored token
     */
    private String refreshToken;
    
    /**
     * Token type (always "Bearer")
     * 
     * VALUE: "Bearer"
     * 
     * PURPOSE: Tells frontend how to use the token
     * USAGE: Authorization: Bearer {token}
     * 
     * WHY "Bearer":
     * - Standard OAuth 2.0 token type
     * - Means "the bearer (holder) of this token has access"
     * - Other types exist (Basic, Digest) but Bearer is most common for JWT
     */
    private String tokenType = "Bearer";
    
    /**
     * Convenience constructor without tokenType
     * (tokenType defaults to "Bearer")
     * 
     * USAGE:
     * new TokenRefreshResponse(newAccessToken, newRefreshToken);
     * 
     * Instead of:
     * new TokenRefreshResponse(newAccessToken, newRefreshToken, "Bearer");
     */
    public TokenRefreshResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}