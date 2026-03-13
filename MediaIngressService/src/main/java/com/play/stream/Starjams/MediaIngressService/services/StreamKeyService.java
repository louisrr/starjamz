package com.play.stream.Starjams.MediaIngressService.services;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.MediaIngressService.dto.StreamKeyRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Generates, validates, and revokes short-lived stream keys.
 *
 * <p>Aerospike schema:
 * <pre>
 *   namespace: fetio
 *   set:       stream_keys
 *   key:       {streamKey}
 *   TTL:       6 hours (auto-expires via Aerospike TTL)
 *   bins:      userId, platform, createdAt (epochMs), expiresAt (epochMs)
 * </pre>
 */
@Service
public class StreamKeyService {

    private static final Logger log = LoggerFactory.getLogger(StreamKeyService.class);
    private static final String NS  = "fetio";
    private static final String SET = "stream_keys";

    private final IAerospikeClient aerospike;

    @Value("${media-ingress.stream-key-ttl-hours:6}")
    private int streamKeyTtlHours;

    public StreamKeyService(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    /**
     * Generates a new stream key tied to the given user and platform.
     * The key is a 32-character hex string (UUID without hyphens).
     */
    public String generateStreamKey(UUID userId, StreamPlatform platform) {
        String streamKey = UUID.randomUUID().toString().replace("-", "");
        long now       = Instant.now().toEpochMilli();
        long expiresAt = now + (streamKeyTtlHours * 3600L * 1000L);

        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.expiration = streamKeyTtlHours * 3600;  // Aerospike TTL in seconds
        wp.sendKey    = true;

        aerospike.put(wp, key,
            new Bin("userId",    userId.toString()),
            new Bin("platform",  platform.name()),
            new Bin("createdAt", now),
            new Bin("expiresAt", expiresAt)
        );

        log.info("Issued stream key {} for user {} platform {}", streamKey, userId, platform);
        return streamKey;
    }

    /**
     * Validates a stream key and returns its metadata.
     *
     * @throws IllegalArgumentException if the key is expired or unknown.
     */
    public StreamKeyRecord validateStreamKey(String streamKey) {
        Key key = new Key(NS, SET, streamKey);
        Record rec = aerospike.get(null, key);

        if (rec == null) {
            throw new IllegalArgumentException("Stream key not found or expired: " + streamKey);
        }

        StreamKeyRecord record = new StreamKeyRecord();
        record.setStreamKey(streamKey);
        record.setUserId(UUID.fromString(rec.getString("userId")));
        record.setPlatform(StreamPlatform.valueOf(rec.getString("platform")));
        record.setCreatedAt(Instant.ofEpochMilli(rec.getLong("createdAt")));
        record.setExpiresAt(Instant.ofEpochMilli(rec.getLong("expiresAt")));
        return record;
    }

    /**
     * Revokes a stream key immediately (e.g., on stream end or admin force-terminate).
     */
    public void revokeStreamKey(String streamKey) {
        Key key = new Key(NS, SET, streamKey);
        aerospike.delete(null, key);
        log.info("Revoked stream key {}", streamKey);
    }

    /** Returns true if the given userId owns this stream key. */
    public boolean isOwner(String streamKey, UUID userId) {
        try {
            StreamKeyRecord rec = validateStreamKey(streamKey);
            return userId.equals(rec.getUserId());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
