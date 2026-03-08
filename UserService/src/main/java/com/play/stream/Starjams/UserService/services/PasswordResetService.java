package com.play.stream.Starjams.UserService.services;

import com.play.stream.Starjams.UserService.dto.ForgotPasswordRequest;
import com.play.stream.Starjams.UserService.dto.PasswordResetRequest;
import com.play.stream.Starjams.UserService.dto.PasswordResetResponse;
import com.play.stream.Starjams.UserService.models.UserModel;

/**
 * Forgot-password / password-reset flow.
 *
 * <p>Sequence:
 * <ol>
 *   <li>Client calls {@link #forgotPassword} with an email or phone number.</li>
 *   <li>Service runs {@code squeryf()} on the contact, looks it up in Aerospike
 *       <em>and</em> ClickHouse, generates an authCode via {@link #randomNumbers()},
 *       stores it in both stores, then calls {@link #userConfirmation} to dispatch
 *       the reset link (email) or auth code (SMS).</li>
 *   <li>User clicks the link → frontend validates the token via
 *       {@link #validateToken} and shows the reset form.</li>
 *   <li>User submits the form → {@link #newPassword} verifies the token,
 *       the confirmEmail factor, updates the password in both stores, and
 *       creates a session token to log the user in.</li>
 * </ol>
 */
public interface PasswordResetService {

    /**
     * Initiate the forgot-password flow.
     *
     * <p>Always returns a generic message regardless of whether the contact exists,
     * to prevent account enumeration attacks.
     *
     * @param request contains the raw email or phone contact (will be sanitized internally)
     * @return generic confirmation message
     */
    PasswordResetResponse forgotPassword(ForgotPasswordRequest request);

    /**
     * Validate a signed reset token without committing any changes.
     * Used by the frontend to gate access to the change-password form.
     *
     * @param token signed token from the reset link
     * @return message indicating validity; null sessionToken
     */
    PasswordResetResponse validateToken(String token);

    /**
     * Update the user's password.
     *
     * <p>Authorization requires <em>either</em>:
     * <ul>
     *   <li>A valid signed reset token + the registered email address as a
     *       second factor (user-initiated path), or</li>
     *   <li>{@code adminApproval = true} + {@code userId} (admin-initiated path).</li>
     * </ul>
     * On success, invalidates the authCode, sets {@code passwordChanged = true},
     * creates an Aerospike session token (24 h TTL), and returns it so the
     * caller can log the user in immediately.
     *
     * @param request contains token or adminApproval, new password, and confirmEmail
     * @return message + session token on success, or error message with null sessionToken
     */
    PasswordResetResponse newPassword(PasswordResetRequest request);

    // ── Utilities exposed so callers can compose them ────────────────────────

    /**
     * Generate a cryptographically random 6-digit auth code (100 000 – 999 999).
     *
     * @return auth code integer
     */
    int randomNumbers();

    /**
     * Dispatch the reset credential to the user via the appropriate channel.
     *
     * <p>Determines the channel by checking whether the user has an email address:
     * <ul>
     *   <li>email present → send reset link via AWS SES</li>
     *   <li>phone only   → send auth code via Twilio SMS</li>
     * </ul>
     * The dispatch uses a branchless function-table gate — no {@code if} statement
     * on the channel selector.
     *
     * @param user      resolved UserModel (must have either email or phoneNumber set)
     * @param resetLink fully-qualified URL containing the signed token
     * @param authCode  6-digit code (included in the SMS payload; embedded in resetLink for email)
     */
    void userConfirmation(UserModel user, String resetLink, int authCode);
}
