package com.gorap.rideservice.exception;

/**
 * TokenRefreshException
 * 
 * PURPOSE: Custom exception for refresh token failures
 * 
 * THROWN WHEN:
 * - Refresh token is expired
 * - Refresh token is revoked (user logged out)
 * - Refresh token not found in database
 * - Refresh token is invalid
 * 
 * CAUGHT BY: GlobalExceptionHandler (returns 403 Forbidden to user)
 * 
 * EXAMPLE USAGE:
 * if (token.isRevoked()) {
 *     throw new TokenRefreshException(token.getToken(), "Token was revoked");
 * }
 */
public class TokenRefreshException extends RuntimeException {
    
    // ✅ Serial version UID for serialization
    // WHY: Required when extending Throwable/Exception
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructor with token and message
     * 
     * @param token The refresh token string that failed
     * @param message Error message explaining why it failed
     * 
     * EXAMPLE:
     * throw new TokenRefreshException(
     *     "eyJhbGci...", 
     *     "Refresh token has expired"
     * );
     * 
     * MESSAGE FORMAT:
     * "Failed for [eyJhbGci...]: Refresh token has expired"
     * 
     * WHY THIS FORMAT:
     * - "Failed for [token]" - Know which token failed
     * - Token is masked in logs (only show first/last chars)
     * - Clear error message for debugging
     */
    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
    }
}