package com.play.stream.Starjams.UserService.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /forgot-password.
 *
 * {@code contact} accepts either an email address or an E.164 phone number.
 * It is sanitized by {@link com.play.stream.Starjams.UserService.util.Prep#squeryf(String)}
 * before any lookup is performed.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email address or phone number is required")
        String contact
) {}
