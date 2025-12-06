package com.gorap.rideservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gorap.rideservice.entity.RefreshToken;
import com.gorap.rideservice.entity.User;

/**
 * RefreshToken Repository
 * 
 * PURPOSE: Database operations for refresh tokens
 * - Find tokens
 * - Delete tokens (logout)
 * - Revoke tokens (security)
 * - Cleanup expired tokens
 * - Count active sessions
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    // ═══════════════════════════════════════════════════════════════
    //                      FIND OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Find refresh token by token string
     * 
     * USE CASE: When user sends refresh token to get new access token
     * 
     * EXAMPLE:
     * POST /api/auth/refresh-token
     * { "refreshToken": "eyJhbGci..." }
     * 
     * Backend: findByToken("eyJhbGci...") -> RefreshToken object
     * 
     * @param token The JWT refresh token string
     * @return Optional containing RefreshToken if found, empty if not found
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Find all tokens for a specific user
     * 
     * USE CASE: Show user their active sessions/devices
     * 
     * EXAMPLE:
     * GET /api/auth/sessions
     * Returns:
     * [
     *   { "device": "iPhone", "createdAt": "2 days ago", "ipAddress": "192.168.1.10" },
     *   { "device": "Chrome", "createdAt": "5 hours ago", "ipAddress": "10.0.0.25" }
     * ]
     * 
     * @param user The user entity
     * @return List of all refresh tokens for this user (active + revoked)
     */
    List<RefreshToken> findByUser(User user);
    
    // ═══════════════════════════════════════════════════════════════
    //                      DELETE OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Delete all tokens for a specific user by userId
     * 
     * USE CASE: 
     * - User deletes account
     * - Admin removes user
     * - Logout from all devices
     * 
     * WHY @Modifying: This query modifies data (DELETE)
     * WHY @Query: Custom JPQL query for efficiency
     * 
     * EXAMPLE:
     * User deletes account -> deleteByUserId(userId)
     * All their tokens are removed from database
     * 
     * @param userId The user's UUID
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = ?1")
    void deleteByUserId(UUID userId);
    
    /**
     * Delete all expired tokens (cleanup job)
     * 
     * USE CASE: Scheduled job runs daily to remove old tokens
     * 
     * WHY: Keep database clean, remove tokens that can't be used anyway
     * 
     * EXAMPLE:
     * @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
     * public void cleanupExpiredTokens() {
     *     refreshTokenRepository.deleteExpiredTokens();
     * }
     * 
     * QUERY EXPLAINED:
     * - DELETE FROM RefreshToken rt
     * - WHERE rt.expiryDate < CURRENT_TIMESTAMP
     * - Removes all tokens where expiry date has passed
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
    
    // ═══════════════════════════════════════════════════════════════
    //                      REVOKE OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Revoke all tokens for a user (logout from all devices)
     * 
     * USE CASE:
     * - User clicks "Logout from all devices" button
     * - Password is changed (security - invalidate all sessions)
     * - Suspicious activity detected
     * 
     * WHY REVOKE instead of DELETE:
     * - Keep audit trail (when user logged out)
     * - Can analyze security patterns
     * - Can show user "You were logged out on..."
     * 
     * WHAT IT DOES:
     * - Sets revoked = true for all user's tokens
     * - Sets revokedAt = current timestamp
     * 
     * EXAMPLE:
     * User changes password:
     * revokeAllUserTokens(userId)
     * -> All devices logged out
     * -> User must login again on all devices
     * 
     * @param userId The user's UUID
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.user.id = ?1")
    void revokeAllUserTokens(UUID userId);
    
    // ═══════════════════════════════════════════════════════════════
    //                      COUNT OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Count active (non-revoked) tokens for a user
     * 
     * USE CASE: Limit concurrent sessions
     * 
     * WHY: Prevent unlimited logins (security + database size)
     * 
     * EXAMPLE:
     * Max sessions = 5
     * Current sessions = countActiveTokensByUserId(userId)
     * 
     * If current == 5:
     *   -> Revoke oldest token
     *   -> Create new token
     * Else:
     *   -> Create new token
     * 
     * QUERY EXPLAINED:
     * - COUNT(rt) = count number of tokens
     * - WHERE rt.user.id = ?1 = for this specific user
     * - AND rt.revoked = false = only active tokens (not logged out)
     * 
     * @param userId The user's UUID
     * @return Number of active tokens (sessions) for this user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = ?1 AND rt.revoked = false")
    long countActiveTokensByUserId(UUID userId);
    
    // ═══════════════════════════════════════════════════════════════
    //                   OPTIONAL: ADVANCED QUERIES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Find all active (non-revoked, non-expired) tokens for a user
     * 
     * USE CASE: Show user their current active sessions
     * 
     * DIFFERENCE from findByUser():
     * - findByUser() returns ALL tokens (active + revoked + expired)
     * - This returns only ACTIVE tokens (can be used right now)
     * 
     * EXAMPLE UI:
     * "Your Active Sessions:"
     * 1. iPhone 13 - Last used 2 hours ago - Chicago, IL
     * 2. Chrome on Windows - Last used now - New York, NY
     * [Logout from this device] button for each
     * 
     * @param user The user entity
     * @return List of active refresh tokens only
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = ?1 AND rt.revoked = false AND rt.expiryDate > CURRENT_TIMESTAMP")
    List<RefreshToken> findActiveTokensByUser(User user);
    
    /**
     * Find oldest active token for a user
     * 
     * USE CASE: When user exceeds max sessions, revoke oldest one
     * 
     * EXAMPLE:
     * User has 5 sessions (max allowed)
     * They login on 6th device
     * -> Find oldest token: findOldestActiveToken(userId)
     * -> Revoke it
     * -> Create new token
     * 
     * QUERY EXPLAINED:
     * - ORDER BY rt.createdAt ASC = oldest first
     * - LIMIT 1 = get only the oldest one
     * 
     * @param userId The user's UUID
     * @return The oldest active token for this user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = ?1 AND rt.revoked = false ORDER BY rt.createdAt ASC")
    List<RefreshToken> findOldestActiveTokens(UUID userId);
}