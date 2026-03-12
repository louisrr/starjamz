package com.play.stream.Starjams.MusicService.services;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Determines which stems a user is allowed to access based on their gift-unlock tier.
 *
 * Tier model (mirrors ViralMechanicsService gift-to-unlock):
 *   Tier 0 (default) — full mastered mix only (FULL_MIX)
 *   Tier 1 (1 gift)  — instrumental + full mix (no VOCALS)
 *   Tier 2 (5 gifts) — all individual stems (VOCALS, DRUMS, BASS, INSTRUMENTS, FULL_MIX)
 *   Tier 3 (20 gifts)— all stems + BPM/key metadata; same stems as Tier 2
 *
 * Unlock state is stored in Aerospike:
 *   namespace: starjamz, set: stem_unlock_state, key: {userId}:{trackId}
 *   bins: tier (long), unlockedAt (epochMs)
 *
 * When a gift is recorded by ViralMechanicsService in FeedService, that service
 * publishes a notification.event of type GIFT_UNLOCK. MusicService does not
 * duplicate the gift logic; it only reads the tier that was already written here
 * (or written externally via recordUnlock).
 */
@Service
public class StemTierResolver {

    private static final Logger log = LoggerFactory.getLogger(StemTierResolver.class);

    private static final String NS  = "starjamz";
    private static final String SET = "stem_unlock_state";

    // Stems accessible per tier
    private static final Set<String> TIER_0_STEMS = Set.of("full_mix");
    private static final Set<String> TIER_1_STEMS = Set.of("full_mix", "instruments");
    private static final Set<String> TIER_2_STEMS = Set.of("full_mix", "vocals", "drums", "bass", "instruments");

    private final IAerospikeClient aerospike;

    public StemTierResolver(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    /**
     * Returns the set of stem types accessible to the given user for this track.
     */
    public Set<String> accessibleStems(UUID userId, UUID trackId) {
        int tier = getUnlockTier(userId, trackId);
        return switch (tier) {
            case 0  -> TIER_0_STEMS;
            case 1  -> TIER_1_STEMS;
            default -> TIER_2_STEMS;  // tier 2 and tier 3 expose the same stems
        };
    }

    /**
     * Returns true if the user may access the requested stem type.
     */
    public boolean canAccessStem(UUID userId, UUID trackId, String stemType) {
        return accessibleStems(userId, trackId).contains(stemType.toLowerCase());
    }

    /**
     * Returns the current unlock tier for a user/track pair (0 if never unlocked).
     */
    public int getUnlockTier(UUID userId, UUID trackId) {
        Key key = new Key(NS, SET, userId.toString() + ":" + trackId.toString());
        Record rec = aerospike.get(null, key, "tier");
        if (rec == null) return 0;
        return (int) rec.getLong("tier");
    }

    /**
     * Records a tier unlock. Called when the gift threshold is met.
     * This is idempotent — re-recording the same tier is a no-op in terms of access.
     */
    public void recordUnlock(UUID userId, UUID trackId, int tier) {
        Key key = new Key(NS, SET, userId.toString() + ":" + trackId.toString());
        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, key,
                new Bin("tier",       (long) tier),
                new Bin("unlockedAt", Instant.now().toEpochMilli()),
                new Bin("userId",     userId.toString()),
                new Bin("trackId",    trackId.toString()));
        log.info("Stem tier {} recorded for userId={} trackId={}", tier, userId, trackId);
    }

    /**
     * Returns the number of gifts required to unlock a given tier.
     */
    public static int giftsRequiredForTier(int tier) {
        return switch (tier) {
            case 1  -> 1;
            case 2  -> 5;
            case 3  -> 20;
            default -> Integer.MAX_VALUE;
        };
    }
}
