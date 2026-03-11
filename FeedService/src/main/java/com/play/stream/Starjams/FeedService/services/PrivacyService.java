package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.FeedService.dto.PrivacySettingsRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stores and retrieves per-user privacy preferences in Aerospike.
 *
 * <p>Aerospike set: {@code user_prefs:{userId}}
 * <pre>
 *   bins: shareLikes, shareViews, sharePlays, shareFollows (0|1)
 * </pre>
 *
 * <p>The fan-out layer calls {@link #canShareActivity} before writing any
 * activity event (TRACK_LIKED, TRACK_PLAYED, etc.) to followers' feeds.
 */
@Service
public class PrivacyService {

    private static final String NS = "fetio";

    private final IAerospikeClient aerospike;

    public PrivacyService(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    public void updateSettings(UUID userId, PrivacySettingsRequest req) {
        Key key = new Key(NS, "user_prefs:" + userId, userId.toString());
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.put(wp, key,
            new Bin("shareLikes",   req.isShareLikes()   ? 1 : 0),
            new Bin("shareViews",   req.isShareViews()   ? 1 : 0),
            new Bin("sharePlays",   req.isSharePlays()   ? 1 : 0),
            new Bin("shareFollows", req.isShareFollows() ? 1 : 0));
    }

    public PrivacySettingsRequest getSettings(UUID userId) {
        Key key = new Key(NS, "user_prefs:" + userId, userId.toString());
        Record rec = aerospike.get(null, key);
        PrivacySettingsRequest settings = new PrivacySettingsRequest();
        if (rec == null) return settings; // defaults are all true
        settings.setShareLikes(rec.getLong("shareLikes") != 0);
        settings.setShareViews(rec.getLong("shareViews") != 0);
        settings.setSharePlays(rec.getLong("sharePlays") != 0);
        settings.setShareFollows(rec.getLong("shareFollows") != 0);
        return settings;
    }

    /**
     * Returns true if the user allows the given activity type to be shared.
     */
    public boolean canShareActivity(UUID userId, String activityType) {
        Key key = new Key(NS, "user_prefs:" + userId, userId.toString());
        Record rec = aerospike.get(null, key);
        if (rec == null) return true; // default: share everything

        return switch (activityType) {
            case "TRACK_LIKED", "VIDEO_LIKED", "PLAYLIST_LIKED" ->
                rec.getLong("shareLikes") != 0;
            case "VIDEO_VIEWED" ->
                rec.getLong("shareViews") != 0;
            case "TRACK_PLAYED" ->
                rec.getLong("sharePlays") != 0;
            case "ARTIST_FOLLOWED" ->
                rec.getLong("shareFollows") != 0;
            default -> true;
        };
    }
}
