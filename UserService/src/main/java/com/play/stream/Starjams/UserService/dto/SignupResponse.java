package com.play.stream.Starjams.UserService.dto;

/**
 * Outbound signup response.
 *
 * {@code userId} is null on rejection (duplicate / invalid contact).
 */
public record SignupResponse(String message, String userId) {}
