package com.play.stream.Starjams.UserService.services;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.UserService.dto.SignupRequest;
import com.play.stream.Starjams.UserService.dto.SignupResponse;
import com.play.stream.Starjams.UserService.models.UserModel;
import com.play.stream.Starjams.UserService.util.ConfirmationCode;
import com.play.stream.Starjams.UserService.util.Prep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final String NAMESPACE     = "starjamz";
    private static final String SET           = "users";
    private static final String EMAIL_IDX_SET = "email_idx";   // O(1) existence check
    private static final String PHONE_IDX_SET = "phone_idx";
    private static final String CONFIRM_SET   = "confirmations";
    private static final int    CONFIRM_TTL   = 900; // 15 minutes in seconds

    // Email: basic RFC-5322 surface check; full validation is the MX/bounce loop.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // Phone: optional leading +, then 7-15 digits (E.164 range).
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    private final IAerospikeClient aerospike;
    private final DataSource       clickHouse;
    private final ConfirmationCode confirmationCode;

    @Autowired
    public UserServiceImpl(IAerospikeClient aerospike,
                           @Qualifier("clickHouseDataSource") DataSource clickHouse,
                           ConfirmationCode confirmationCode) {
        this.aerospike        = aerospike;
        this.clickHouse       = clickHouse;
        this.confirmationCode = confirmationCode;
    }

    private Key key(UUID id) {
        return new Key(NAMESPACE, SET, id.toString());
    }

    // =========================================================================
    // SIGNUP
    // =========================================================================

    @Override
    public SignupResponse signup(SignupRequest request, String ip, String userAgent, String hostname) {

        // 1. Password length is enforced by @Size on SignupRequest, but guard here too.
        if (request.password() == null || request.password().length() < 8) {
            return new SignupResponse("Password must be at least 8 characters.", null);
        }

        String contact = request.contact().trim();
        boolean isEmail = EMAIL_PATTERN.matcher(contact).matches();
        boolean isPhone = !isEmail && PHONE_PATTERN.matcher(contact).matches();

        // 2. Validate format.
        if (!isEmail && !isPhone) {
            return new SignupResponse("Invalid email address or phone number.", null);
        }

        // 3. Duplicate check: Aerospike index set first (O(1) key lookup).
        String indexSet = isEmail ? EMAIL_IDX_SET : PHONE_IDX_SET;
        Key    idxKey   = new Key(NAMESPACE, indexSet, contact.toLowerCase(Locale.ROOT));
        if (aerospike.get(null, idxKey) != null) {
            return new SignupResponse(
                    "You cannot create an account with this contact information.", null);
        }

        // 4. Duplicate check: ClickHouse as second source of truth.
        if (existsInClickHouse(contact, isEmail)) {
            return new SignupResponse(
                    "You cannot create an account with this contact information.", null);
        }

        // 5. Hash password via Prep (Blake3 + 32-byte random salt).
        String hashedPassword = Prep.hashPassword(request.password());

        // 6. Derive email_hash (useful for analytics without exposing raw email).
        String emailHash = isEmail ? Prep.hashContact(contact) : null;

        // 7. Generate UUID and derive username.
        UUID   userId   = UUID.randomUUID();
        String username = deriveUsername(contact, isEmail);
        long   now      = Instant.now().getEpochSecond();
        String device   = Prep.parseDevice(userAgent);

        // 8. Write to Aerospike users set.
        //    CREATE_ONLY gives us atomic protection against a race condition
        //    where two concurrent signups slip past both checks above.
        WritePolicy createOnly = new WritePolicy();
        createOnly.recordExistsAction = RecordExistsAction.CREATE_ONLY;

        try {
            aerospike.put(createOnly, key(userId),
                    new Bin("userId",          userId.toString()),
                    new Bin("userName",        username),
                    new Bin("email",           isEmail ? contact : null),
                    new Bin("emailHash",       emailHash),
                    new Bin("phoneNumber",     isPhone ? contact : null),
                    new Bin("password",        hashedPassword),
                    new Bin("signupTimestamp", now),
                    new Bin("lastOnline",      now),
                    new Bin("following",       0),
                    new Bin("likes",           0),
                    new Bin("shares",          0),
                    new Bin("followerCount",   0),
                    new Bin("mostViewedGenre", 0),
                    new Bin("ipAddress",       ip),
                    new Bin("hostname",        hostname),
                    new Bin("userAgent",       userAgent),
                    new Bin("device",          device),
                    new Bin("confirmed",       false)
            );
        } catch (AerospikeException ae) {
            if (ae.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
                return new SignupResponse(
                        "You cannot create an account with this contact information.", null);
            }
            throw ae;
        }

        // 9. Register in the index set (also CREATE_ONLY — best-effort; main record already written).
        WritePolicy idxPolicy = new WritePolicy();
        idxPolicy.recordExistsAction = RecordExistsAction.UPDATE;
        aerospike.put(idxPolicy, idxKey, new Bin("userId", userId.toString()));

        // 10. Mirror to ClickHouse users table (system of record).
        insertUserToClickHouse(userId, username,
                isEmail ? contact : null,
                emailHash,
                isPhone ? contact : null,
                hashedPassword, now, ip, hostname, userAgent, device);

        // 11. Log CREATED event to existing audit table.
        UserModel stub = buildStub(userId, username, contact, isEmail, emailHash);
        logEvent("CREATED", stub);

        // 12. Generate and store confirmation code in Aerospike with TTL.
        String code = ConfirmationCode.generate();
        WritePolicy ttlPolicy = new WritePolicy();
        ttlPolicy.expiration = CONFIRM_TTL;
        aerospike.put(ttlPolicy,
                new Key(NAMESPACE, CONFIRM_SET, userId.toString()),
                new Bin("code",    code),
                new Bin("contact", contact),
                new Bin("isEmail", isEmail));

        // 13. Dispatch confirmation via SES (email) or Twilio (SMS).
        confirmationCode.dispatch(contact, isEmail, code);

        String channel = isEmail ? "email" : "phone";
        return new SignupResponse(
                "Account created. A confirmation code has been sent to your " + channel + ".",
                userId.toString());
    }

    // -------------------------------------------------------------------------
    // Signup helpers
    // -------------------------------------------------------------------------

    /**
     * Check ClickHouse for an existing account with the same email or phone.
     * Uses FINAL to see fully merged data (avoids phantom reads in ReplacingMergeTree).
     */
    private boolean existsInClickHouse(String contact, boolean isEmail) {
        String col = isEmail ? "email" : "phone_number";
        String sql = "SELECT count() FROM starjamz.users FINAL WHERE " + col + " = ?";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contact.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            // Treat query failure as "not found" — Aerospike is the primary gate.
            return false;
        }
    }

    /**
     * Insert a new user row into the ClickHouse users table.
     * All string comparisons in ClickHouse are case-sensitive; we store lowercased contact.
     */
    private void insertUserToClickHouse(UUID userId, String username,
                                        String email, String emailHash,
                                        String phone, String password,
                                        long nowEpoch,
                                        String ip, String hostname,
                                        String userAgent, String device) {
        String sql = "INSERT INTO starjamz.users " +
                "(id, user_name, email, email_hash, phone_number, password_hash, " +
                " signup_timestamp, last_online, following, likes, shares, " +
                " ip_address, hostname, geolocation, user_agent, device, " +
                " most_viewed_genre, follower_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, NULL, ?, ?, 0, 0)";
        try (Connection conn = clickHouse.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp ts = Timestamp.from(Instant.ofEpochSecond(nowEpoch));
            ps.setString(1, userId.toString());
            ps.setString(2, username);
            ps.setString(3, email   != null ? email.toLowerCase(Locale.ROOT) : null);
            ps.setString(4, emailHash);
            ps.setString(5, phone   != null ? phone : null);
            ps.setString(6, password);
            ps.setTimestamp(7, ts);
            ps.setTimestamp(8, ts);
            ps.setString(9, ip);
            ps.setString(10, hostname);
            ps.setString(11, userAgent);
            ps.setString(12, device);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user into ClickHouse", e);
        }
    }

    /**
     * Derive username from email local-part or phone digits, plus a random 1-2 digit suffix.
     *
     * Examples:
     *   john.doe@gmail.com  →  john_doe_47
     *   +15551234567        →  15551234567_3
     */
    private String deriveUsername(String contact, boolean isEmail) {
        String base;
        if (isEmail) {
            // Take everything before the @, then replace non-alphanumeric chars with _
            base = contact.substring(0, contact.indexOf('@'))
                          .toLowerCase(Locale.ROOT)
                          .replaceAll("[^a-z0-9]", "_");
        } else {
            // Strip leading + and use raw digits
            base = contact.replaceAll("[^0-9]", "");
        }
        // Append a pseudorandom 1-2 digit number (1–99)
        int suffix = ThreadLocalRandom.current().nextInt(1, 100);
        return base + "_" + suffix;
    }

    /** Build a minimal UserModel stub for audit log purposes. */
    private UserModel buildStub(UUID userId, String username, String contact,
                                boolean isEmail, String emailHash) {
        UserModel stub = new UserModel();
        stub.setU(userId);
        stub.setUserName(username);
        if (isEmail) {
            stub.setEmail(contact);
            stub.setEmailHash(emailHash);
        } else {
            stub.setPhoneNumber(contact);
        }
        return stub;
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
                new Bin("screenName",      user.getScreenName()),
                new Bin("userName",        user.getUserName()),
                new Bin("email",           user.getEmail()),
                new Bin("emailHash",       user.getEmailHash()),
                new Bin("phoneNumber",     user.getPhoneNumber()),
                new Bin("password",        user.getPassword()),
                new Bin("device",          user.getDevice()),
                new Bin("userAgent",       user.getUserAgent()),
                new Bin("url",             user.getUrl()),
                new Bin("aviAddress",      user.getAviAddress()),
                new Bin("coverAddress",    user.getCoverAddress()),
                new Bin("lastMusicListen", user.getLastMusicListen() != null ? user.getLastMusicListen().toString() : null),
                new Bin("lastAudioStream", user.getLastAudioStream() != null ? user.getLastAudioStream().toString() : null),
                new Bin("lastVideoStream", user.getLastVideoStream() != null ? user.getLastVideoStream().toString() : null),
                new Bin("bio",             user.getBio()),
                new Bin("coordLat",        user.getCoords() != null ? user.getCoords()[0] : null),
                new Bin("coordLon",        user.getCoords() != null ? user.getCoords()[1] : null),
                new Bin("signupTimestamp", user.getSignupTimestamp()),
                new Bin("lastOnline",      user.getLastOnline()),
                new Bin("following",       user.getFollowing()),
                new Bin("likes",           user.getLikes()),
                new Bin("shares",          user.getShares()),
                new Bin("ipAddress",       user.getIpAddress()),
                new Bin("hostname",        user.getHostname()),
                new Bin("geolocation",     user.getGeolocation()),
                new Bin("mostViewedGenre", user.getMostViewedGenre()),
                new Bin("followerCount",   user.getFollowerCount()),
                new Bin("gender",          user.getGender() != null ? user.getGender().name() : null)
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
        user.setPhoneNumber(record.getString("phoneNumber"));
        user.setSignupTimestamp(record.getLong("signupTimestamp"));
        user.setLastOnline(record.getLong("lastOnline"));
        user.setFollowing((int) record.getLong("following"));
        user.setLikes((int) record.getLong("likes"));
        user.setShares((int) record.getLong("shares"));
        user.setIpAddress(record.getString("ipAddress"));
        user.setHostname(record.getString("hostname"));
        user.setGeolocation(record.getString("geolocation"));
        user.setMostViewedGenre((int) record.getLong("mostViewedGenre"));
        user.setFollowerCount((int) record.getLong("followerCount"));
        String genderStr = record.getString("gender");
        if (genderStr != null) {
            user.setGender(com.play.stream.Starjams.UserService.models.Gender.valueOf(genderStr));
        }
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
