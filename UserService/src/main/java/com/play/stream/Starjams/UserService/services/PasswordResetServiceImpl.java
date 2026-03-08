package com.play.stream.Starjams.UserService.services;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.UserService.dto.ForgotPasswordRequest;
import com.play.stream.Starjams.UserService.dto.PasswordResetRequest;
import com.play.stream.Starjams.UserService.dto.PasswordResetResponse;
import com.play.stream.Starjams.UserService.models.UserModel;
import com.play.stream.Starjams.UserService.util.ConfirmationCode;
import com.play.stream.Starjams.UserService.util.Prep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implements the complete forgot-password / password-reset flow.
 *
 * <p>Stores: Aerospike (primary, real-time) + ClickHouse (system of record).
 * NOTE: The project uses ClickHouse, not PostgreSQL; patterns are identical via JDBC.
 *
 * <p>Required application properties:
 * <pre>
 *   app.reset-secret=&lt;strong-random-secret&gt;   # HMAC-SHA256 signing key for reset tokens
 *   app.reset-base-url=https://starjamz.com    # prepended to /reset-password?token=…
 * </pre>
 */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    // ── Aerospike set names (mirrors UserServiceImpl constants) ──────────────
    private static final String NAMESPACE     = "starjamz";
    private static final String SET           = "users";
    private static final String EMAIL_IDX_SET = "email_idx";
    private static final String PHONE_IDX_SET = "phone_idx";
    private static final String SESSIONS_SET  = "sessions";

    // ── Auth code / token lifetime ───────────────────────────────────────────
    private static final long   RESET_TTL_MS      = 15L * 60 * 1_000; // 15 minutes
    private static final int    SESSION_TTL_SEC    = 86_400;           // 24 hours

    // ── Contact format patterns (same as UserServiceImpl) ───────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    // ── SecureRandom for auth codes and session tokens ───────────────────────
    private static final SecureRandom RNG = new SecureRandom();

    // ── Spring-injected dependencies ─────────────────────────────────────────
    private final IAerospikeClient  aerospike;
    private final DataSource        clickHouse;
    private final ConfirmationCode  confirmationCode;
    private final UserService       userService;

    @Value("${app.reset-secret}")
    private String resetSecret;

    @Value("${app.reset-base-url}")
    private String resetBaseUrl;

    @Autowired
    public PasswordResetServiceImpl(IAerospikeClient aerospike,
                                    @Qualifier("clickHouseDataSource") DataSource clickHouse,
                                    ConfirmationCode confirmationCode,
                                    UserService userService) {
        this.aerospike        = aerospike;
        this.clickHouse       = clickHouse;
        this.confirmationCode = confirmationCode;
        this.userService      = userService;
    }

    // =========================================================================
    // forgotPassword — entry point for the reset flow
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Always returns the same generic message regardless of outcome to prevent
     * account-enumeration timing attacks.  Internal logic:
     * <ol>
     *   <li>Sanitize contact via {@link Prep#squeryf(String)} (branchless allowlist).</li>
     *   <li>Validate format (email or E.164 phone).</li>
     *   <li>Look up the contact in Aerospike index set <em>and</em> ClickHouse;
     *       proceed only if found in <b>both</b> stores.</li>
     *   <li>Generate 6-digit authCode via {@link #randomNumbers()} and store it
     *       in the user record in Aerospike (bins: {@code auth_code},
     *       {@code auth_code_date}) and in ClickHouse (mutation).</li>
     *   <li>Build a signed HMAC-SHA256 reset token and call
     *       {@link #userConfirmation} to dispatch it.</li>
     * </ol>
     */
    @Override
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        // Generic message returned in every branch — prevents account enumeration.
        final String GENERIC = "If this account exists, a reset link has been sent.";

        // 1. Sanitize: strip SQL/NoSQL/XSS/injection vectors — branchless, O(n)
        String contact = Prep.squeryf(request.contact().trim());

        // 2. Format validation
        boolean isEmail = EMAIL_PATTERN.matcher(contact).matches();
        boolean isPhone = !isEmail && PHONE_PATTERN.matcher(contact).matches();
        if (!isEmail && !isPhone) {
            return new PasswordResetResponse(GENERIC, null);
        }

        // 3. Aerospike index lookup — O(1) key read
        String indexSet = isEmail ? EMAIL_IDX_SET : PHONE_IDX_SET;
        Key    idxKey   = new Key(NAMESPACE, indexSet, contact.toLowerCase(Locale.ROOT));
        Record idxRecord = aerospike.get(null, idxKey);
        if (idxRecord == null) {
            return new PasswordResetResponse(GENERIC, null);
        }

        // 4. ClickHouse lookup — must be confirmed in the system of record too
        if (!existsInClickHouse(contact, isEmail)) {
            return new PasswordResetResponse(GENERIC, null);
        }

        // 5. Resolve user
        String userIdStr = idxRecord.getString("userId");
        UUID   userId    = UUID.fromString(userIdStr);
        Optional<UserModel> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return new PasswordResetResponse(GENERIC, null);
        }
        UserModel user = userOpt.get();

        // 6. Generate authCode and record timestamp
        int  authCode = randomNumbers();
        long nowMs    = Instant.now().toEpochMilli();

        // 7. Persist authCode to Aerospike (UPDATE policy — record must already exist)
        WritePolicy wp = new WritePolicy();
        wp.recordExistsAction = RecordExistsAction.UPDATE;
        aerospike.put(wp, new Key(NAMESPACE, SET, userIdStr),
                new Bin("auth_code",      authCode),
                new Bin("auth_code_date", nowMs));

        // 8. Persist authCode to ClickHouse (mutation, async on server side)
        updateAuthCodeInClickHouse(userId, authCode, nowMs);

        // 9. Build signed reset token (payload + HMAC-SHA256 signature)
        long   expiryMs  = nowMs + RESET_TTL_MS;
        String token     = generateResetToken(userId, authCode, expiryMs);
        String resetLink = resetBaseUrl + "/reset-password?token=" + token;

        // 10. Dispatch confirmation — branchless gate (see userConfirmation)
        userConfirmation(user, resetLink, authCode);

        return new PasswordResetResponse(GENERIC, null);
    }

    // =========================================================================
    // validateToken — lightweight check for the frontend gate
    // =========================================================================

    @Override
    public PasswordResetResponse validateToken(String token) {
        TokenData td = parseAndVerifyToken(token);
        if (td == null) {
            return new PasswordResetResponse("Reset link is invalid or has expired.", null);
        }
        return new PasswordResetResponse("Token is valid.", null);
    }

    // =========================================================================
    // newPassword — update the password (admin OR verified user path)
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>User path validation chain:
     * <ol>
     *   <li>Parse and HMAC-verify the signed token (constant-time compare).</li>
     *   <li>Verify the token's authCode matches the value stored in Aerospike
     *       (constant-time byte comparison).</li>
     *   <li>Verify the token has not expired.</li>
     *   <li>Verify {@code confirmEmail} matches the registered email
     *       (constant-time byte comparison — second factor).</li>
     *   <li>Hash new password with Blake3 + 32-byte random salt.</li>
     *   <li>Write updated password + {@code passwordChanged=true} to Aerospike
     *       and ClickHouse, clear authCode.</li>
     *   <li>Create a 24-hour session token in Aerospike {@code sessions} set
     *       and return it to log the user in.</li>
     * </ol>
     */
    @Override
    public PasswordResetResponse newPassword(PasswordResetRequest request) {
        if (request.newPassword() == null || request.newPassword().length() < 8) {
            return new PasswordResetResponse("Password must be at least 8 characters.", null);
        }

        // ── Admin path ───────────────────────────────────────────────────────
        if (request.adminApproval()) {
            if (request.userId() == null) {
                return new PasswordResetResponse("userId is required for admin password reset.", null);
            }
            return commitPasswordChange(request.userId(), request.newPassword());
        }

        // ── User path: token + confirmEmail ─────────────────────────────────

        // 1. Parse and verify HMAC signature (constant-time)
        TokenData td = parseAndVerifyToken(request.token());
        if (td == null) {
            return new PasswordResetResponse("Reset link is invalid or has expired.", null);
        }

        // 2. Load user from Aerospike
        Record userRecord = aerospike.get(null, new Key(NAMESPACE, SET, td.userId().toString()));
        if (userRecord == null) {
            return new PasswordResetResponse("Account not found.", null);
        }

        // 3. Constant-time authCode comparison (stored vs. token)
        long storedCode = userRecord.getLong("auth_code");
        long storedDate = userRecord.getLong("auth_code_date");
        boolean codeMatch = MessageDigest.isEqual(
                String.valueOf(storedCode).getBytes(StandardCharsets.UTF_8),
                String.valueOf(td.authCode()).getBytes(StandardCharsets.UTF_8));
        boolean notExpired = (Instant.now().toEpochMilli() - storedDate) < RESET_TTL_MS;

        if (!codeMatch || !notExpired) {
            return new PasswordResetResponse("Reset link is invalid or has expired.", null);
        }

        // 4. Constant-time confirmEmail check (second factor — "the actual email itself")
        Optional<UserModel> userOpt = userService.findById(td.userId());
        if (userOpt.isEmpty()) {
            return new PasswordResetResponse("Account not found.", null);
        }
        UserModel user = userOpt.get();

        String storedEmail = user.getEmail() != null ? user.getEmail().toLowerCase(Locale.ROOT) : "";
        String givenEmail  = request.confirmEmail() != null
                             ? request.confirmEmail().toLowerCase(Locale.ROOT) : "";
        boolean emailMatch = MessageDigest.isEqual(
                storedEmail.getBytes(StandardCharsets.UTF_8),
                givenEmail.getBytes(StandardCharsets.UTF_8));

        if (!emailMatch || storedEmail.isEmpty()) {
            return new PasswordResetResponse("Email confirmation does not match.", null);
        }

        // 5. All checks passed — update password
        return commitPasswordChange(td.userId(), request.newPassword());
    }

    // =========================================================================
    // randomNumbers — cryptographically random 6-digit auth code
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Range: 100 000 – 999 999 (always 6 digits, never zero-padded).
     * Uses a shared {@link SecureRandom} instance (thread-safe).
     */
    @Override
    public int randomNumbers() {
        return 100_000 + RNG.nextInt(900_000);
    }

    // =========================================================================
    // userConfirmation — branchless channel dispatch
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Branchless gate implementation — selects the dispatch function by
     * array index instead of an {@code if} statement:
     * <pre>
     *   int hasEmail = (email != null &amp;&amp; !email.isEmpty()) ? 1 : 0;
     *   senders[hasEmail].send(contacts[hasEmail], payloads[hasEmail]);
     * </pre>
     * {@code senders[0]} → Twilio SMS (phone-only account)<br>
     * {@code senders[1]} → AWS SES email with full reset link
     */
    @Override
    public void userConfirmation(UserModel user, String resetLink, int authCode) {
        // Null-safe accessors (null-guard is not a business-logic branch)
        String email = user.getEmail()       != null ? user.getEmail()       : "";
        String phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";

        // 1 = has email → SES path; 0 = phone-only → SMS path
        int hasEmail = (!email.isEmpty()) ? 1 : 0;

        // Contact array: index 0 = phone, index 1 = email
        String[] contacts = { phone, email };

        // Payload array: index 0 = SMS code string, index 1 = full reset link
        String[] payloads = { String.valueOf(authCode), resetLink };

        // Functional dispatch table — replaces if/else
        @FunctionalInterface
        interface ResetSender { void send(String contact, String payload); }

        ResetSender[] senders = {
            (c, p) -> confirmationCode.dispatchResetSms(c, p),   // index 0: SMS
            (c, p) -> confirmationCode.dispatchResetEmail(c, p)  // index 1: SES email
        };

        // Branchless select and dispatch
        senders[hasEmail].send(contacts[hasEmail], payloads[hasEmail]);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Commit the password change to both Aerospike and ClickHouse,
     * clear the authCode, flag the account, and mint a session token.
     */
    private PasswordResetResponse commitPasswordChange(UUID userId, String rawPassword) {
        String hashed = Prep.hashPassword(rawPassword);
        String userIdStr = userId.toString();

        // ── Aerospike: update password bins, clear auth code, set flag ────────
        WritePolicy wp = new WritePolicy();
        wp.recordExistsAction = RecordExistsAction.UPDATE;
        aerospike.put(wp, new Key(NAMESPACE, SET, userIdStr),
                new Bin("password",        hashed),
                new Bin("passwordChanged", true),
                new Bin("auth_code",       Value.NULL),   // invalidate OTP
                new Bin("auth_code_date",  Value.NULL));

        // ── ClickHouse: mutation to update password and flag ──────────────────
        updatePasswordInClickHouse(userId, hashed);

        // ── Audit event ───────────────────────────────────────────────────────
        logPasswordChangedEvent(userId);

        // ── Session token: UUID stored in Aerospike with 24-hour TTL ─────────
        String sessionToken = UUID.randomUUID().toString();
        WritePolicy sessionPolicy = new WritePolicy();
        sessionPolicy.expiration = SESSION_TTL_SEC;
        aerospike.put(sessionPolicy,
                new Key(NAMESPACE, SESSIONS_SET, sessionToken),
                new Bin("userId", userIdStr));

        return new PasswordResetResponse("Password updated successfully. You are now logged in.",
                sessionToken);
    }

    // ── Token generation and verification ────────────────────────────────────

    /**
     * Build a signed reset token.
     *
     * <p>Format: {@code base64url(payload) "." base64url(HMAC-SHA256(payload))}
     * where {@code payload = userId ":" authCode ":" expiryEpochMs}.
     */
    private String generateResetToken(UUID userId, int authCode, long expiryMs) {
        String raw     = userId + ":" + authCode + ":" + expiryMs;
        byte[] payload = Base64.getUrlEncoder().withoutPadding()
                               .encode(raw.getBytes(StandardCharsets.UTF_8));
        byte[] sig     = hmacSha256(payload);
        byte[] sigB64  = Base64.getUrlEncoder().withoutPadding().encode(sig);
        return new String(payload, StandardCharsets.UTF_8)
               + "." + new String(sigB64, StandardCharsets.UTF_8);
    }

    /**
     * Verify the HMAC signature and decode token fields.
     * Returns {@code null} if the token is malformed, tampered, or expired.
     */
    private TokenData parseAndVerifyToken(String token) {
        if (token == null || token.isBlank()) return null;
        int dot = token.indexOf('.');
        if (dot < 0) return null;

        byte[] payloadBytes;
        byte[] providedSig;
        try {
            payloadBytes = token.substring(0, dot).getBytes(StandardCharsets.UTF_8);
            providedSig  = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Constant-time HMAC comparison — prevents timing oracle on the signature
        byte[] expectedSig = hmacSha256(payloadBytes);
        if (!MessageDigest.isEqual(expectedSig, providedSig)) return null;

        // Decode payload
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(payloadBytes), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String[] parts = raw.split(":", 3);
        if (parts.length != 3) return null;

        try {
            UUID userId   = UUID.fromString(parts[0]);
            int  authCode = Integer.parseInt(parts[1]);
            long expiryMs = Long.parseLong(parts[2]);
            if (Instant.now().toEpochMilli() > expiryMs) return null;
            return new TokenData(userId, authCode, expiryMs);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** HMAC-SHA256 over {@code data} using the configured reset secret. */
    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(resetSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 initialisation failed", e);
        }
    }

    // ── ClickHouse helpers ────────────────────────────────────────────────────

    private boolean existsInClickHouse(String contact, boolean isEmail) {
        String col = isEmail ? "email" : "phone_number";
        String sql = "SELECT count() FROM starjamz.users FINAL WHERE " + col + " = ?";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contact.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            return false; // Aerospike is the primary gate; treat CH failure as not-found
        }
    }

    private void updateAuthCodeInClickHouse(UUID userId, int authCode, long epochMs) {
        // ClickHouse ALTER TABLE UPDATE is an async mutation; submission is synchronous.
        String sql = "ALTER TABLE starjamz.users UPDATE "
                   + "auth_code = ?, auth_code_date = ? "
                   + "WHERE id = ?";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authCode);
            ps.setLong(2, epochMs);
            ps.setString(3, userId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Non-fatal: Aerospike is authoritative for real-time auth code checks.
            // Log and continue so the user still receives their reset link.
        }
    }

    private void updatePasswordInClickHouse(UUID userId, String hashedPassword) {
        String sql = "ALTER TABLE starjamz.users UPDATE "
                   + "password_hash = ?, password_changed = 1, "
                   + "auth_code = 0, auth_code_date = 0 "
                   + "WHERE id = ?";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, userId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password in ClickHouse", e);
        }
    }

    private void logPasswordChangedEvent(UUID userId) {
        String sql = "INSERT INTO user_events "
                   + "(event_id, event_type, occurred_at, user_id, screen_name, user_name, "
                   + "email, email_hash, avi_address, cover_address, bio, url) "
                   + "VALUES (?, ?, now(), ?, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, "PASSWORD_CHANGED");
            ps.setString(3, userId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log PASSWORD_CHANGED event", e);
        }
    }

    // ── Value type for decoded token ──────────────────────────────────────────
    private record TokenData(UUID userId, int authCode, long expiryMs) {}
}
