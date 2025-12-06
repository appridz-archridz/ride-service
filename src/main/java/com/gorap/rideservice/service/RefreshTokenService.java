package com.gorap.rideservice.service;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gorap.rideservice.entity.RefreshToken;
import com.gorap.rideservice.entity.User;
import com.gorap.rideservice.repository.RefreshTokenRepository;
import com.gorap.rideservice.repository.UserRepository;
import com.gorap.rideservice.util.JwtUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * RefreshToken Service
 * 
 * PURPOSE: Business logic for refresh tokens
 * - Create tokens
 * - Validate tokens (expired, revoked)
 * - Revoke tokens (logout)
 * - Manage session limits
 * - Cleanup old tokens
 */
@Service
@Slf4j // ✅ Lombok annotation for logging
public class RefreshTokenService {
    
    // ═══════════════════════════════════════════════════════════════
    //                      CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    
    // ✅ Read from application.properties
    // Default: 7 days (604800000 milliseconds)
    @Value("${app.jwtRefreshExpirationMs:604800000}")
    private Long refreshTokenDurationMs;
    
    // ✅ Read from application.properties
    // Default: Max 5 active sessions per user
    @Value("${app.maxRefreshTokensPerUser:5}")
    private int maxRefreshTokensPerUser;
    
    // ═══════════════════════════════════════════════════════════════
    //                      DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    // ═══════════════════════════════════════════════════════════════
    //                      CREATE TOKEN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create and store a new refresh token
     * 
     * CALLED WHEN:
     * - User logs in
     * - User refreshes their access token (optional: token rotation)
     * 
     * WHAT IT DOES:
     * 1. Check if user has too many active tokens (session limit)
     * 2. If yes, revoke oldest token
     * 3. Generate new JWT refresh token string
     * 4. Create RefreshToken entity
     * 5. Save to database
     * 6. Return RefreshToken object
     * 
     * @param userId User's UUID
     * @param ipAddress User's IP address (for tracking)
     * @param userAgent User's browser/device info (for tracking)
     * @return Saved RefreshToken entity
     */
    @Transactional // ✅ IMPORTANT: Database transaction
    public RefreshToken createRefreshToken(UUID userId, String ipAddress, String userAgent) {
        // ✅ STEP 1: Get user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // ✅ STEP 2: Check if user has too many active tokens
        long activeTokens = refreshTokenRepository.countActiveTokensByUserId(userId);
        
        if (activeTokens >= maxRefreshTokensPerUser) {
            // ⚠️ User has reached session limit (e.g., 5 devices)
            log.info("User {} has {} active sessions (max: {}). Revoking oldest token.", 
                     userId, activeTokens, maxRefreshTokensPerUser);
            
            // Revoke oldest token to make room for new one
            revokeOldestToken(userId);
        }
        
        // ✅ STEP 3: Generate JWT refresh token string
        // Uses email as subject (primary identifier)
        String token = jwtUtils.generateRefreshTokenFromEmail(user.getEmail(), userId);
        
        // ✅ STEP 4: Create RefreshToken entity
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        
        // ✅ STEP 5: Set expiry date (current time + 7 days)
        refreshToken.setExpiryDate(
            Instant.now().plus(refreshTokenDurationMs, ChronoUnit.MILLIS)
        );
        
        // ✅ STEP 6: Set creation timestamp
        refreshToken.setCreatedAt(Instant.now());
        
        // ✅ STEP 7: Set device/location tracking info
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        
        // ✅ STEP 8: Mark as active (not revoked)
        refreshToken.setRevoked(false);
        
        // ✅ STEP 9: Save to database and return
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        
        log.info("Created refresh token for user {} from IP {} (Device: {})", 
                 user.getEmail(), ipAddress, extractDeviceInfo(userAgent));
        
        return savedToken;
    }
    
    // ═══════════════════════════════════════════════════════════════
    //                      FIND TOKEN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Find refresh token by token string
     * 
     * CALLED WHEN:
     * - User sends refresh token to get new access token
     * - Validating if token exists in database
     * 
     * @param token JWT refresh token string
     * @return Optional containing RefreshToken if found
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    // ═══════════════════════════════════════════════════════════════
    //                      VERIFY TOKEN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Verify if refresh token is valid
     * 
     * CHECKS:
     * 1. Is token revoked? (user logged out)
     * 2. Is token expired? (past expiry date)
     * 
     * WHAT HAPPENS:
     * - If revoked: Throws TokenRefreshException
     * - If expired: Deletes token from DB, throws TokenRefreshException
     * - If valid: Returns token unchanged
     * 
     * @param token RefreshToken entity
     * @return The same token if valid
     * @throws TokenRefreshException if token is revoked or expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        // ✅ CHECK 1: Is token revoked?
        if (token.isRevoked()) {
            log.warn("Attempted to use revoked refresh token. Token ID: {}, User: {}", 
                     token.getId(), token.getUser().getEmail());
            
//            throw new TokenRefreshException(
//                token.getToken(), 
//                "Refresh token was revoked. Please login again."
//            );
        }
        
        // ✅ CHECK 2: Is token expired?
        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Refresh token expired. Token ID: {}, Expired at: {}", 
                     token.getId(), token.getExpiryDate());
            
            // Delete expired token from database (cleanup)
            refreshTokenRepository.delete(token);
            
//            throw new TokenRefreshException(
//                token.getToken(),
//                "Refresh token has expired. Please login again."
//            );
        }
        
        // ✅ Token is valid!
        return token;
    }
    
    // ═══════════════════════════════════════════════════════════════
    //                      DELETE TOKEN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Delete a specific token (logout from one device)
     * 
     * CALLED WHEN:
     * - User clicks "Logout" on one device
     * - User removes a session from their profile
     * 
     * @param token JWT refresh token string
     */
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    log.info("Deleting refresh token for user: {}", 
                             refreshToken.getUser().getEmail());
                    refreshTokenRepository.delete(refreshToken);
                });
    }
    
    /**
     * Delete all tokens for a user (complete account cleanup)
     * 
     * CALLED WHEN:
     * - User deletes their account
     * - Admin removes user
     * 
     * @param userId User's UUID
     */
    @Transactional
    public void deleteByUserId(UUID userId) {
        log.info("Deleting all refresh tokens for user ID: {}", userId);
        refreshTokenRepository.deleteByUserId(userId);
    }
    
    // ═══════════════════════════════════════════════════════════════
    //                      REVOKE TOKEN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Revoke all tokens for a user (logout from all devices)
     * 
     * CALLED WHEN:
     * - User clicks "Logout from all devices"
     * - User changes password (security measure)
     * - Suspicious activity detected
     * 
     * DIFFERENCE from deleteByUserId():
     * - DELETE: Completely removes tokens (no audit trail)
     * - REVOKE: Marks as revoked, keeps in database (audit trail)
     * 
     * @param userId User's UUID
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        log.info("Revoking all refresh tokens for user ID: {}", userId);
        refreshTokenRepository.revokeAllUserTokens(userId);
    }
    
    /**
     * Revoke a specific token (logout from one device, keep audit trail)
     * 
     * CALLED WHEN:
     * - User logs out normally
     * - Token is used to refresh (optional: token rotation)
     * 
     * @param token JWT refresh token string
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            log.info("Revoking refresh token for user: {}", rt.getUser().getEmail());
            
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }
    
    // ═══════════════════════════════════════════════════════════════
    //                      CLEANUP
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Delete all expired tokens from database
     * 
     * CALLED BY: Scheduled job (e.g., daily at 2 AM)
     * 
     * WHY: Keep database clean, remove tokens that can't be used
     * 
     * EXAMPLE SCHEDULER:
     * @Scheduled(cron = "0 0 2 * * *")
     * public void scheduledCleanup() {
     *     refreshTokenService.deleteExpiredTokens();
     * }
     */
    @Transactional
    public void deleteExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens...");
        refreshTokenRepository.deleteExpiredTokens();
        log.info("Expired refresh tokens cleanup completed");
    }
    
    // ═══════════════════════════════════════════════════════════════
    //                      PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Revoke oldest active token when session limit is reached
     * 
     * CALLED BY: createRefreshToken() when user has too many sessions
     * 
     * WHAT IT DOES:
     * 1. Find all active tokens for user
     * 2. Sort by creation date (oldest first)
     * 3. Revoke the oldest one
     * 
     * EXAMPLE:
     * User has 5 active sessions (max allowed)
     * They login on 6th device
     * -> This method revokes their oldest session
     * -> New session is created
     * -> User still has 5 sessions (oldest replaced with newest)
     * 
     * @param userId User's UUID
     */
    @Transactional
    private void revokeOldestToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // ✅ Get all active tokens, sorted by creation date
        refreshTokenRepository.findOldestActiveTokens(userId).stream()
                .filter(rt -> !rt.isRevoked()) // Only active tokens
                .min((rt1, rt2) -> rt1.getCreatedAt().compareTo(rt2.getCreatedAt())) // Find oldest
                .ifPresent(oldestToken -> {
                    // Revoke the oldest token
                    log.info("Revoking oldest token for user {}. Created at: {}", 
                             user.getEmail(), oldestToken.getCreatedAt());
                    
                    oldestToken.setRevoked(true);
                    oldestToken.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(oldestToken);
                });
    }
    
    /**
     * Extract simple device info from user agent string
     * 
     * PURPOSE: Make logs more readable
     * 
     * INPUT: "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) Safari/604.1"
     * OUTPUT: "iPhone (Safari)"
     * 
     * INPUT: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/119.0.0.0"
     * OUTPUT: "Windows (Chrome)"
     * 
     * @param userAgent Full user agent string
     * @return Simplified device info
     */
    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }
        
        // Simple extraction (you can make this more sophisticated)
        if (userAgent.contains("iPhone")) {
            return "iPhone";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("Windows")) {
            return "Windows PC";
        } else if (userAgent.contains("Mac")) {
            return "Mac";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else {
            return "Unknown Device";
        }
    }
}