package com.gorap.rideservice.request;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TokenRefreshRequest DTO
 * 
 * PURPOSE: Request body for refresh token endpoint
 * 
 * ENDPOINT: POST /api/auth/refresh-token
 * 
 * REQUEST BODY:
 * {
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 * 
 * VALIDATION:
 * - refreshToken must not be blank/null/empty
 */
@Data  // ✅ Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // ✅ Lombok: Generates no-args constructor (required by Jackson)
@AllArgsConstructor  // ✅ Lombok: Generates all-args constructor (convenient for testing)
public class TokenRefreshRequest {
    
    /**
     * The refresh token JWT string
     * 
     * EXAMPLE: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXNlcklkIjoiMTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc0MDAwIiwidHlwZSI6IlJFRlJFU0giLCJpYXQiOjE2OTk4NzY1NDMsImV4cCI6MTcwMDQ4MTM0M30.K7x2mP9qR4tY6wZ8vC1nM5bX3hG7jL0sA4fD9kE2pQ6r"
     * 
     * VALIDATION:
     * @NotBlank - Ensures:
     *   - Not null
     *   - Not empty string ""
     *   - Not only whitespace "   "
     * 
     * ERROR MESSAGE: "Refresh token is required"
     * 
     * WHY VALIDATE:
     * - Prevent null pointer exceptions
     * - Give clear error to user
     * - Fail fast (don't waste time on invalid requests)
     */
     private String userAgent;
    private String refreshToken;
}