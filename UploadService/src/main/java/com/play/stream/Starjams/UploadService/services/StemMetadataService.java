package com.play.stream.Starjams.UploadService.services;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads and writes stem metadata in Aerospike.
 *
 * Aerospike layout:
 *   namespace : starjamz
 *   set       : stems
 *   key       : {trackId}:{stemType}   e.g. "a1b2c3:vocals"
 *   bins      : s3Key, uploadedAt, durationMs
 */
@Service
public class StemMetadataService {

    private static final Logger log = LoggerFactory.getLogger(StemMetadataService.class);
    private static final String NS  = "starjamz";
    private static final String SET = "stems";

    private final IAerospikeClient aerospike;

    public StemMetadataService(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    /**
     * Persist all stem S3 keys for a track after Demucs separation.
     *
     * @param trackId   track UUID string
     * @param stems     map of stemType → s3Key
     * @param durationMs approximate duration of the track in milliseconds (0 if unknown)
     */
    public void saveStems(String trackId, Map<String, String> stems, long durationMs) {
        WritePolicy wp = new WritePolicy();
        long uploadedAt = Instant.now().toEpochMilli();

        for (Map.Entry<String, String> entry : stems.entrySet()) {
            String stemType = entry.getKey().toLowerCase();
            String s3Key    = entry.getValue();

            Key key = new Key(NS, SET, trackId + ":" + stemType);
            aerospike.put(wp, key,
                    new Bin("s3Key",      s3Key),
                    new Bin("uploadedAt", uploadedAt),
                    new Bin("durationMs", durationMs),
                    new Bin("trackId",    trackId),
                    new Bin("stemType",   stemType));

            log.debug("Saved stem metadata: track={} type={} key={}", trackId, stemType, s3Key);
        }
    }

    /**
     * Retrieve the S3 key for a specific stem, or null if not found.
     */
    public String getStemS3Key(String trackId, String stemType) {
        Key key = new Key(NS, SET, trackId + ":" + stemType.toLowerCase());
        Record record = aerospike.get(null, key, "s3Key");
        if (record == null) return null;
        return record.getString("s3Key");
    }

    /**
     * Retrieve all available stems for a track as a map of stemType → s3Key.
     */
    public Map<String, String> getAllStems(String trackId) {
        String[] stemTypes = {"vocals", "drums", "bass", "instruments", "full_mix"};
        Map<String, String> result = new HashMap<>();
        for (String stemType : stemTypes) {
            String s3Key = getStemS3Key(trackId, stemType);
            if (s3Key != null) {
                result.put(stemType, s3Key);
            }
        }
        return result;
    }
}
