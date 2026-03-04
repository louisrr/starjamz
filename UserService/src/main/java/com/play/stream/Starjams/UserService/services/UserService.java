package com.play.stream.Starjams.UserService.services;

import com.play.stream.Starjams.UserService.dto.SignupRequest;
import com.play.stream.Starjams.UserService.dto.SignupResponse;
import com.play.stream.Starjams.UserService.models.UserModel;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    /**
     * Full signup flow:
     * validate → duplicate-check → hash password → store → send confirmation.
     *
     * @param request   email/phone + password
     * @param ip        caller's IP address
     * @param userAgent caller's User-Agent header
     * @param hostname  reverse-DNS hostname of the caller (may equal ip if unavailable)
     * @return SignupResponse with a message and, on success, the new user UUID
     */
    SignupResponse signup(SignupRequest request, String ip, String userAgent, String hostname);

    // Create a new user
    UserModel createUser(UserModel user);

    // Find a user by ID (UUID)
    Optional<UserModel> findById(UUID id);

    // Update a user in ScyllaDB
    UserModel updateUser(UserModel user);

    // Add an avatar address to the user by the UUID
    UserModel addAvatarAddress(UUID userId, String avatarAddress);

    // Change the email, if the user is logged in
    UserModel changeEmail(UUID userId, String newEmail);

    // Add a secondary email address
    UserModel addSecondaryEmail(UUID userId, String secondaryEmail);

    // Add a bio
    UserModel addBio(UUID userId, String bio);

    // Delete the user
    void deleteUser(UUID userId);
}
