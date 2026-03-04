package com.play.stream.Starjams.UserService.services;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.UserService.models.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final String NAMESPACE = "starjamz";
    private static final String SET = "users";

    private final IAerospikeClient aerospike;
    private final DataSource clickHouse;

    @Autowired
    public UserServiceImpl(IAerospikeClient aerospike,
                           @Qualifier("clickHouseDataSource") DataSource clickHouse) {
        this.aerospike = aerospike;
        this.clickHouse = clickHouse;
    }

    private Key key(UUID id) {
        return new Key(NAMESPACE, SET, id.toString());
    }

    private void logEvent(String eventType, UserModel user) {
        String sql = "INSERT INTO user_events " +
                "(event_id, event_type, occurred_at, user_id, screen_name, user_name, " +
                "email, email_hash, avi_address, cover_address, bio, url) " +
                "VALUES (?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, eventType);
            ps.setString(3, user.getId() != null ? user.getId().toString() : null);
            ps.setString(4, user.getScreenName());
            ps.setString(5, user.getUserName());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getEmailHash());
            ps.setString(8, user.getAviAddress());
            ps.setString(9, user.getCoverAddress());
            ps.setString(10, user.getBio());
            ps.setString(11, user.getUrl());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log user event to ClickHouse", e);
        }
    }

    @Override
    public UserModel createUser(UserModel user) {
        if (user.getId() == null) {
            user.setU(UUID.randomUUID());
        }
        aerospike.put(new WritePolicy(), key(user.getId()),
                new Bin("screenName", user.getScreenName()),
                new Bin("userName", user.getUserName()),
                new Bin("email", user.getEmail()),
                new Bin("emailHash", user.getEmailHash()),
                new Bin("password", user.getPassword()),
                new Bin("device", user.getDevice()),
                new Bin("userAgent", user.getUserAgent()),
                new Bin("url", user.getUrl()),
                new Bin("aviAddress", user.getAviAddress()),
                new Bin("coverAddress", user.getCoverAddress()),
                new Bin("lastMusicListen", user.getLastMusicListen() != null ? user.getLastMusicListen().toString() : null),
                new Bin("lastAudioStream", user.getLastAudioStream() != null ? user.getLastAudioStream().toString() : null),
                new Bin("lastVideoStream", user.getLastVideoStream() != null ? user.getLastVideoStream().toString() : null),
                new Bin("bio", user.getBio()),
                new Bin("coordLat", user.getCoords() != null ? user.getCoords()[0] : null),
                new Bin("coordLon", user.getCoords() != null ? user.getCoords()[1] : null)
        );
        logEvent("CREATED", user);
        return user;
    }

    @Override
    public Optional<UserModel> findById(UUID id) {
        Record record = aerospike.get(null, key(id));
        if (record == null) {
            return Optional.empty();
        }
        UserModel user = new UserModel();
        user.setU(id);
        user.setScreenName(record.getString("screenName"));
        user.setUserName(record.getString("userName"));
        user.setEmail(record.getString("email"));
        user.setEmailHash(record.getString("emailHash"));
        user.setPassword(record.getString("password"));
        user.setDevice(record.getString("device"));
        user.setUserAgent(record.getString("userAgent"));
        user.setUrl(record.getString("url"));
        user.setAviAddress(record.getString("aviAddress"));
        user.setCoverAddress(record.getString("coverAddress"));
        String lastMusic = record.getString("lastMusicListen");
        if (lastMusic != null) user.setLastMusicListen(UUID.fromString(lastMusic));
        String lastAudio = record.getString("lastAudioStream");
        if (lastAudio != null) user.setLastAudioStream(UUID.fromString(lastAudio));
        String lastVideo = record.getString("lastVideoStream");
        if (lastVideo != null) user.setLastVideoStream(UUID.fromString(lastVideo));
        user.setBio(record.getString("bio"));
        Double lat = (Double) record.getValue("coordLat");
        Double lon = (Double) record.getValue("coordLon");
        if (lat != null && lon != null) {
            user.setCoords(new double[]{lat, lon});
        }
        return Optional.of(user);
    }

    @Override
    public UserModel updateUser(UserModel user) {
        aerospike.put(new WritePolicy(), key(user.getId()),
                new Bin("screenName", user.getScreenName()),
                new Bin("userName", user.getUserName()),
                new Bin("email", user.getEmail()),
                new Bin("emailHash", user.getEmailHash()),
                new Bin("password", user.getPassword()),
                new Bin("device", user.getDevice()),
                new Bin("userAgent", user.getUserAgent()),
                new Bin("url", user.getUrl()),
                new Bin("aviAddress", user.getAviAddress()),
                new Bin("coverAddress", user.getCoverAddress()),
                new Bin("lastMusicListen", user.getLastMusicListen() != null ? user.getLastMusicListen().toString() : null),
                new Bin("lastAudioStream", user.getLastAudioStream() != null ? user.getLastAudioStream().toString() : null),
                new Bin("lastVideoStream", user.getLastVideoStream() != null ? user.getLastVideoStream().toString() : null),
                new Bin("bio", user.getBio()),
                new Bin("coordLat", user.getCoords() != null ? user.getCoords()[0] : null),
                new Bin("coordLon", user.getCoords() != null ? user.getCoords()[1] : null)
        );
        logEvent("UPDATED", user);
        return user;
    }

    @Override
    public UserModel addAvatarAddress(UUID userId, String avatarAddress) {
        aerospike.put(new WritePolicy(), key(userId), new Bin("aviAddress", avatarAddress));
        UserModel user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        logEvent("UPDATED", user);
        return user;
    }

    @Override
    public UserModel changeEmail(UUID userId, String newEmail) {
        aerospike.put(new WritePolicy(), key(userId), new Bin("email", newEmail));
        UserModel user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        logEvent("UPDATED", user);
        return user;
    }

    @Override
    public UserModel addSecondaryEmail(UUID userId, String secondaryEmail) {
        aerospike.put(new WritePolicy(), key(userId), new Bin("secondaryEmail", secondaryEmail));
        UserModel user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        logEvent("UPDATED", user);
        return user;
    }

    @Override
    public UserModel addBio(UUID userId, String bio) {
        aerospike.put(new WritePolicy(), key(userId), new Bin("bio", bio));
        UserModel user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        logEvent("UPDATED", user);
        return user;
    }

    @Override
    public void deleteUser(UUID userId) {
        UserModel user = findById(userId).orElseGet(() -> {
            UserModel stub = new UserModel();
            stub.setU(userId);
            return stub;
        });
        aerospike.delete(new WritePolicy(), key(userId));
        logEvent("DELETED", user);
    }
}
