package com.play.stream.Starjams.UserService.util;

import org.bouncycastle.crypto.digests.Blake3Digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Prep - reusable cryptographic utilities.
 *
 * Passwords are stored as:  base64(salt) + ":" + hex(Blake3(salt || password))
 * Contact hashes are stored as: hex(Blake3(contact))
 *
 * Blake3 is used because it is:
 *   - Significantly faster than SHA-2 on modern hardware
 *   - Collision-resistant with 256-bit output
 *   - Suitable for keyed/salted hashing
 *
 * NOTE: Blake3 is a fast cryptographic hash, not a password-hardening function
 * (like Argon2). The random 32-byte salt prevents rainbow-table attacks.
 * If regulatory requirements demand a memory-hard KDF, swap hashPassword() to Argon2.
 */
public final class Prep {

    private static final int SALT_BYTES   = 32;
    private static final int OUTPUT_BITS  = 256; // 32-byte Blake3 output
    private static final int OUTPUT_BYTES = OUTPUT_BITS / 8;

    // One SecureRandom instance shared across threads; nextBytes() is thread-safe.
    private static final SecureRandom RNG = new SecureRandom();

    private Prep() {}

    // -------------------------------------------------------------------------
    // Password hashing
    // -------------------------------------------------------------------------

    /**
     * Hash a plaintext password with a freshly generated 32-byte random salt.
     *
     * @param password raw plaintext (must be >= 8 chars; caller validates)
     * @return "base64(salt):hex(hash)" — safe to store in the database
     */
    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        byte[] hash = blake3(salt, password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    /**
     * Constant-time verification of a plaintext password against a stored hash.
     *
     * @param password raw plaintext
     * @param stored   value previously returned by {@link #hashPassword}
     * @return true if the password matches
     */
    public static boolean verifyPassword(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return false;
        byte[] salt         = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = HexFormat.of().parseHex(parts[1]);
        byte[] actualHash   = blake3(salt, password.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(expectedHash, actualHash); // constant-time compare
    }

    // -------------------------------------------------------------------------
    // Contact (email / phone) hashing — used for email_hash column
    // -------------------------------------------------------------------------

    /**
     * Produce a deterministic hex hash of a contact identifier (email or phone).
     * Useful for lookups without exposing the raw value in analytics tables.
     *
     * @param contact email address or phone number (normalised/trimmed before calling)
     * @return hex string
     */
    public static String hashContact(String contact) {
        byte[] input = contact.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        Blake3Digest digest = new Blake3Digest(OUTPUT_BITS);
        digest.update(input, 0, input.length);
        byte[] out = new byte[OUTPUT_BYTES];
        digest.doFinal(out, 0);
        return HexFormat.of().formatHex(out);
    }

    // -------------------------------------------------------------------------
    // User-agent device classification
    // -------------------------------------------------------------------------

    /**
     * Derive a coarse device category from a raw User-Agent string.
     *
     * @param userAgent raw HTTP User-Agent header value
     * @return "mobile" | "tablet" | "desktop" | "unknown"
     */
    public static String parseDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "unknown";
        String ua = userAgent.toLowerCase(java.util.Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet")) return "tablet";
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "mobile";
        return "desktop";
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Blake3(salt || password) → 32-byte digest */
    private static byte[] blake3(byte[] salt, byte[] password) {
        Blake3Digest digest = new Blake3Digest(OUTPUT_BITS);
        digest.update(salt, 0, salt.length);
        digest.update(password, 0, password.length);
        byte[] out = new byte[OUTPUT_BYTES];
        digest.doFinal(out, 0);
        return out;
    }
}
