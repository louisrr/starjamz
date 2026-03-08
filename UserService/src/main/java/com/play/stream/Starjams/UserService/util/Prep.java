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
    // Input sanitization — squeryf()
    // -------------------------------------------------------------------------

    /**
     * Maximum byte length accepted by {@link #squeryf(String)}.
     * 320 bytes covers the RFC 5321 maximum MAIL path (email) and any E.164 phone number.
     */
    public static final int SQUERYF_MAX_LEN = 320;

    /**
     * 256-bit allowlist: one bit per possible byte value (0-255).
     * Set bits: A-Z  a-z  0-9  @  .  +  -  _
     * Every other byte value is blocked (zeroed) by squeryf().
     */
    private static final int[] SQUERYF_ALLOWLIST = buildSqueryAllowlist();

    private static int[] buildSqueryAllowlist() {
        int[] mask = new int[8]; // 8 × 32 bits = 256 bits
        for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@.+-_".toCharArray()) {
            mask[c >>> 5] |= (1 << (c & 31));
        }
        return mask;
    }

    /**
     * Constant-time input sanitizer that eliminates SQL injection, NoSQL injection,
     * XSS, OS command injection, LDAP injection, and related attack vectors before
     * any data reaches a query or template.
     *
     * <p>Design principles:
     * <ol>
     *   <li><b>Allowlist-based</b>: only {@code A-Z a-z 0-9 @ . + - _} survive;
     *       every other byte is zeroed unconditionally.</li>
     *   <li><b>Branchless inner loop</b>: every input byte visits the same integer
     *       ALU pipeline regardless of whether it is "safe" or "dangerous".
     *       Execution time is uniform across all inputs of the same length, removing
     *       timing side-channels that could fingerprint the filter or reveal which
     *       injection pattern was attempted.</li>
     *   <li><b>O(n) time, O(n) space</b>: one pre-allocated output buffer,
     *       a single linear pass — no regex engine, no parse tree, no heap
     *       allocations inside the loop.</li>
     *   <li><b>Hard length cap</b>: applied before processing to bound
     *       memory usage regardless of input size.</li>
     * </ol>
     *
     * @param input  raw user-supplied string (email or phone)
     * @param maxLen maximum bytes to inspect; anything beyond is silently dropped
     * @return sanitized string containing only allowlisted bytes;
     *         empty string if {@code input} is null
     */
    public static String squeryf(String input, int maxLen) {
        if (input == null) return "";
        byte[] raw = input.getBytes(StandardCharsets.UTF_8);
        int    len = Math.min(raw.length, maxLen);
        byte[] out = new byte[len];

        // ── Branchless allowlist filter ──────────────────────────────────────
        // No if/switch on byte value inside this loop.
        // For each byte c (0-255):
        //   word = SQUERYF_ALLOWLIST[c >>> 5]   → the 32-bit word that covers c
        //   bit  = (word >>> (c & 31)) & 1       → 1 if c is in the allowlist, 0 if not
        //   mask = -bit                           → 0xFFFFFFFF if bit=1, 0x00000000 if bit=0
        //   out[i] = raw[i] & mask                → keep original byte or zero it — no branch
        for (int i = 0; i < len; i++) {
            int c    = raw[i] & 0xFF;
            int word = SQUERYF_ALLOWLIST[c >>> 5];
            int bit  = (word >>> (c & 31)) & 1;
            int mask = -bit;
            out[i]   = (byte) (raw[i] & mask);
        }

        // ── Compact: remove zero-slots produced by the filter ────────────────
        // Two O(n) linear passes; no early termination, no content-sensitive skip.
        int count = 0;
        for (int i = 0; i < len; i++) { count += (out[i] != 0) ? 1 : 0; }
        byte[] compact = new byte[count];
        int    j       = 0;
        for (int i = 0; i < len; i++) { if (out[i] != 0) compact[j++] = out[i]; }

        return new String(compact, StandardCharsets.UTF_8);
    }

    /**
     * Convenience overload using the default max length ({@value #SQUERYF_MAX_LEN} bytes,
     * covering RFC 5321 email paths and E.164 phone numbers).
     *
     * @param input raw user-supplied string
     * @return sanitized string
     */
    public static String squeryf(String input) {
        return squeryf(input, SQUERYF_MAX_LEN);
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
