package com.gorap.rideservice.entity;


import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RefreshToken Entity
 * 
 * PURPOSE: Store refresh tokens in database for:
 * - Token revocation (logout)
 * - Session management (limit active devices)
 * - Security tracking (IP, user agent)
 * - Audit trail
 * 
 * TABLE: refresh_tokens
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    // ✅ PRIMARY KEY
    // WHY UUID: More secure than auto-increment IDs
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    // ✅ RELATIONSHIP: Which user owns this token
    // WHY @ManyToOne: One user can have multiple refresh tokens (multiple devices)
    // CASCADE: When user is deleted, all their tokens are deleted too
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // ✅ THE ACTUAL JWT REFRESH TOKEN STRING
    // WHY unique=true: Each token can only exist once in database
    // WHY length=500: JWT tokens can be long (200-500 characters)
    // EXAMPLE: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1..."
    @Column(nullable = false, unique = true, length = 500)
    private String token;
    
    // ✅ WHEN TOKEN EXPIRES
    // WHY Instant: More precise than Date, timezone-safe
    // EXAMPLE: 2024-11-09T10:30:45Z (7 days from creation)
    @Column(nullable = false)
    private Instant expiryDate;
    
    // ✅ WHEN TOKEN WAS CREATED
    // WHY: Track age of tokens, useful for analytics
    // EXAMPLE: 2024-11-02T10:30:45Z
    @Column(nullable = false)
    private Instant createdAt;
    
    // ✅ WHEN TOKEN WAS REVOKED (if revoked)
    // WHY nullable: Only set when token is revoked (logout)
    // EXAMPLE: 2024-11-05T15:20:30Z (when user logged out)
    @Column
    private Instant revokedAt;
    
    // ✅ IS TOKEN REVOKED?
    // WHY: Quick check without checking revokedAt
    // TRUE = Token is revoked (can't be used anymore)
    // FALSE = Token is active (can be used)
    @Column(nullable = false)
    private boolean revoked = false;
    
    // ✅ IP ADDRESS of the device/user
    // WHY: Security - track where logins happen
    // WHY length=45: IPv6 addresses can be up to 45 characters
    // EXAMPLES: 
    // - IPv4: "192.168.1.100"
    // - IPv6: "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
    @Column(length = 45)
    private String ipAddress;
    
    // ✅ USER AGENT (browser/device info)
    // WHY: Know which device/browser is logged in
    // EXAMPLES:
    // - "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/119.0.0.0"
    // - "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) Safari/604.1"
    // - "PostmanRuntime/7.32.3" (for API testing)
    @Column(length = 255)
    private String userAgent;
}