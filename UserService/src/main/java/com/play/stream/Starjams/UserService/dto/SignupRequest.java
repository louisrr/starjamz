package com.play.stream.Starjams.UserService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound signup payload.
 *
 * {@code contact} is either an email address or an E.164 phone number.
 * The service layer decides which one it is and validates the format.
 */
public record SignupRequest(

        @NotBlank(message = "Email address or phone number is required")
        String contact,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
