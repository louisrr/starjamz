package com.play.stream.Starjams.UserService.services;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.play.stream.Starjams.UserService.models.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final CqlSession session;

    @Autowired
    public UserServiceImpl(CqlSession session) {
        this.session = session;
    }

    @Override
    public UserModel createUser(UserModel user) {
        String query = String.format("INSERT INTO users (id, email, avi_address, bio) VALUES (%s, '%s', '%s', '%s')",
                user.getId(), user.getEmail(), user.getAviAddress(), user.getBio());
        session.execute(query);
        return user;
    }

    @Override
    public Optional<UserModel> findById(UUID id) {
        String query = String.format("SELECT * FROM users WHERE id = %s", id);
        ResultSet resultSet = session.execute(query);
        Row row = resultSet.one();
        if (row != null) {
            UserModel user = new UserModel();
            user.setEmail(row.getString("email"));
            user.setAviAddress(row.getString("avi_address"));
            user.setBio(row.getString("bio"));
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public UserModel updateUser(UserModel user) {
        String query = String.format("UPDATE users SET email = '%s', avi_address = '%s', bio = '%s' WHERE id = %s",
                user.getEmail(), user.getAviAddress(), user.getBio(), user.getId());
        session.execute(query);
        return user;
    }

    @Override
    public UserModel addAvatarAddress(UUID userId, String avatarAddress) {
        String query = String.format("UPDATE users SET avi_address = '%s' WHERE id = %s", avatarAddress, userId);
        session.execute(query);
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    @Override
    public UserModel changeEmail(UUID userId, String newEmail) {
        String query = String.format("UPDATE users SET email = '%s' WHERE id = %s", newEmail, userId);
        session.execute(query);
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    @Override
    public UserModel addSecondaryEmail(UUID userId, String secondaryEmail) {
        // Assuming there's a column for secondary email in your users table.
        String query = String.format("UPDATE users SET secondary_email = '%s' WHERE id = %s", secondaryEmail, userId);
        session.execute(query);
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    @Override
    public UserModel addBio(UUID userId, String bio) {
        String query = String.format("UPDATE users SET bio = '%s' WHERE id = %s", bio, userId);
        session.execute(query);
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    @Override
    public void deleteUser(UUID userId) {
        String query = String.format("DELETE FROM users WHERE id = %s", userId);
        session.execute(query);
    }
}
