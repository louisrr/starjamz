package com.play.stream.Starjams.UserService.dto;

/**
 * Response from POST /forgot-password and POST /reset-password.
 *
 * <p>{@code sessionToken} is non-null only on a successful password change
 * (step 8 of the reset flow). The client should store it as an HttpOnly cookie
 * or in memory and send it as a Bearer token on subsequent requests. The
 * GatewayService validates it against the Aerospike {@code sessions} set.
 */
public record PasswordResetResponse(
        String message,
        String sessionToken
) {}
