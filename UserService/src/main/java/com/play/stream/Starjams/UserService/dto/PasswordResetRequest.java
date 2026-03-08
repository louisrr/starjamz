package com.play.stream.Starjams.UserService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for POST /reset-password.
 *
 * <p>Two mutually exclusive authorization paths:
 * <ol>
 *   <li><b>Admin path</b>: set {@code adminApproval = true} and supply {@code userId}.
 *       {@code token} and {@code confirmEmail} are ignored.</li>
 *   <li><b>User path</b>: supply the signed {@code token} from the reset email/SMS,
 *       plus the account's registered {@code confirmEmail} as a second factor.</li>
 * </ol>
 */
public record PasswordResetRequest(
        /** Signed reset token from the emailed link (user path only). */
        String token,

        /** New plaintext password — must be >= 8 characters. */
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,

        /**
         * The registered email address of the account (user path only).
         * Must match the email stored in the account as a second verification factor.
         */
        String confirmEmail,

        /** Set true when called from the admin service to bypass token validation. */
        boolean adminApproval,

        /** Required when {@code adminApproval} is true. */
        UUID userId
) {}
