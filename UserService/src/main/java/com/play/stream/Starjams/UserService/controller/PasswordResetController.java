package com.play.stream.Starjams.UserService.controller;

import com.play.stream.Starjams.UserService.dto.ForgotPasswordRequest;
import com.play.stream.Starjams.UserService.dto.PasswordResetRequest;
import com.play.stream.Starjams.UserService.dto.PasswordResetResponse;
import com.play.stream.Starjams.UserService.services.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes three endpoints for the forgot-password / reset-password flow.
 *
 * <pre>
 *   POST /forgot-password          — step 1: initiate reset, send link or code
 *   GET  /reset-password/validate  — step 2: frontend validates token before showing form
 *   POST /reset-password           — step 3: submit new password (user or admin path)
 * </pre>
 *
 * <p>All inputs pass through {@link com.play.stream.Starjams.UserService.util.Prep#squeryf}
 * inside the service layer before any database interaction.
 */
@RestController
@RequestMapping
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Initiate forgot-password flow.
     *
     * <p>Accepts an email address or E.164 phone number. Always returns HTTP 202
     * with a generic message — success and failure are intentionally indistinguishable
     * to callers to prevent account-enumeration attacks.
     *
     * <p>On internal success: stores authCode in Aerospike + ClickHouse, then
     * dispatches a reset link (email) or auth code (SMS) to the user.
     *
     * @param request {@code { "contact": "user@example.com" }}
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        PasswordResetResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Validate a reset token without mutating any state.
     *
     * <p>The frontend calls this when the user lands on the reset-password page
     * (after clicking the emailed link) to verify the token is still valid before
     * rendering the new-password form.
     *
     * <p>Returns 200 if valid, 400 if invalid or expired.
     *
     * @param token signed token from the reset link query string
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<PasswordResetResponse> validateToken(
            @RequestParam String token) {
        PasswordResetResponse response = passwordResetService.validateToken(token);
        boolean valid = response.sessionToken() != null
                        || response.message().equals("Token is valid.");
        return valid
               ? ResponseEntity.ok(response)
               : ResponseEntity.badRequest().body(response);
    }

    /**
     * Submit a new password.
     *
     * <p><b>User path</b> (normal reset):
     * <pre>
     * {
     *   "token":        "&lt;signed-token-from-link&gt;",
     *   "newPassword":  "myNewP@ssw0rd",
     *   "confirmEmail": "user@example.com",
     *   "adminApproval": false
     * }
     * </pre>
     *
     * <p><b>Admin path</b> (admin service forcing a reset):
     * <pre>
     * {
     *   "adminApproval": true,
     *   "userId":        "550e8400-e29b-41d4-a716-446655440000",
     *   "newPassword":   "temporaryP@ss1"
     * }
     * </pre>
     *
     * <p>Returns 200 + {@code sessionToken} on success (logs the user in),
     * or 400 with an error message on failure.
     *
     * @param request password reset payload
     */
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        PasswordResetResponse response = passwordResetService.newPassword(request);
        boolean success = response.sessionToken() != null;
        return success
               ? ResponseEntity.ok(response)
               : ResponseEntity.badRequest().body(response);
    }
}
