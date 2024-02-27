package com.play.stream.Starjams.UserService.services;

import com.play.stream.Starjams.UserService.models.UserModel;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    // Create a new user
    UserModel createUser(UserModel user);

    // Find a user by ID (UUID)
    Optional<UserModel> findById(UUID id);

    // Update a user in ScyllaDB
    UserModel updateUser(UserModel user);

    // Add an avatar address to the user by the UUID
    UserModel addAvatarAddress(UUID userId, String avatarAddress);

    // Change the email, if the user is logged in
    // This operation typically involves more context about user session,
    // which is not directly related to a database operation
    UserModel changeEmail(UUID userId, String newEmail);

    // Add a secondary email address
    // Assuming there's a mechanism to distinguish primary and secondary emails,
    // otherwise, it's just updating/adding another email field
    UserModel addSecondaryEmail(UUID userId, String secondaryEmail);

    // Add a bio
    UserModel addBio(UUID userId, String bio);

    // Delete the user
    void deleteUser(UUID userId);
}
