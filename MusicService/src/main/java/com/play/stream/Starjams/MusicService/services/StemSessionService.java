package com.play.stream.Starjams.MusicService.services;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.MusicService.entity.StemRoyaltyLedger;
import com.play.stream.Starjams.MusicService.repository.StemRoyaltyLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Manages collaborative stem sessions: shared rooms for 2–8 listeners who
 * co-control the stem mix in real time.
 *
 * Session state is stored in Aerospike:
 *   namespace: starjamz, set: stem_sessions, key: {sessionId}
 *   bins:
 *     hostUserId    (String)
 *     participants  (JSON list of userIds)
 *     stemVolumes   (JSON map of stemType → volume)
 *     startedAt     (epochMs)
 *     trackId       (String)
 *
 * Micro-royalty accumulation:
 *   namespace: starjamz, set: stem_session_royalties, key: {sessionId}:{listenerUserId}
 *   bins: listenerMinutes (double), hostUserId (String)
 *
 * Hourly scheduled job flushes accumulated royalties to PostgreSQL.
 */
@Service
public class StemSessionService {

    private static final Logger log = LoggerFactory.getLogger(StemSessionService.class);

    private static final String NS             = "starjamz";
    private static final String SESSION_SET    = "stem_sessions";
    private static final String ROYALTY_SET    = "stem_session_royalties";
    private static final int    MAX_PARTICIPANTS = 8;
    private static final double ROYALTY_PER_LISTENER_MINUTE = 0.001; // USD

    private final IAerospikeClient          aerospike;
    private final StemRoyaltyLedgerRepository royaltyRepo;
    private final ObjectMapper              objectMapper;

    public StemSessionService(IAerospikeClient aerospike,
                               StemRoyaltyLedgerRepository royaltyRepo,
                               ObjectMapper objectMapper) {
        this.aerospike   = aerospike;
        this.royaltyRepo = royaltyRepo;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    /**
     * Creates a new collaborative session. The host must hold at least Tier 2 access.
     * Returns the new sessionId.
     */
    public UUID createSession(UUID trackId, UUID hostUserId,
                               Map<String, Double> initialVolumes) {
        UUID sessionId = UUID.randomUUID();

        Map<String, Double> volumes = (initialVolumes != null)
                ? initialVolumes
                : Map.of("vocals", 1.0, "drums", 1.0, "bass", 1.0, "instruments", 1.0);

        WritePolicy wp = new WritePolicy();
        Key key = new Key(NS, SESSION_SET, sessionId.toString());

        aerospike.put(wp, key,
                new Bin("hostUserId",   hostUserId.toString()),
                new Bin("participants", toJson(List.of(hostUserId.toString()))),
                new Bin("stemVolumes",  toJson(volumes)),
                new Bin("startedAt",    Instant.now().toEpochMilli()),
                new Bin("trackId",      trackId.toString()));

        log.info("Created stem session {} for track {} host {}", sessionId, trackId, hostUserId);
        return sessionId;
    }

    /**
     * Adds a participant to an existing session (max 8).
     * Returns false if session is full or not found.
     */
    public boolean joinSession(UUID sessionId, UUID userId) {
        Key key = new Key(NS, SESSION_SET, sessionId.toString());
        Record rec = aerospike.get(null, key);
        if (rec == null) return false;

        List<String> participants = fromJson(rec.getString("participants"),
                new TypeReference<>() {});
        if (participants.size() >= MAX_PARTICIPANTS) return false;
        if (participants.contains(userId.toString())) return true; // already in

        participants.add(userId.toString());
        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, key, new Bin("participants", toJson(participants)));
        log.info("User {} joined session {}", userId, sessionId);
        return true;
    }

    /**
     * Updates the shared stem volumes for the session and records listener-minute
     * royalty accumulation for the host.
     */
    public Map<String, Double> updateSessionMix(UUID sessionId, UUID participantId,
                                                 Map<String, Double> newVolumes) {
        Key key = new Key(NS, SESSION_SET, sessionId.toString());
        Record rec = aerospike.get(null, key);
        if (rec == null) throw new NoSuchElementException("Session not found: " + sessionId);

        String hostUserId = rec.getString("hostUserId");
        List<String> participants = fromJson(rec.getString("participants"),
                new TypeReference<>() {});

        if (!participants.contains(participantId.toString())) {
            throw new IllegalStateException("User " + participantId + " is not in session " + sessionId);
        }

        // Merge volumes (patch semantics — only update keys provided)
        Map<String, Double> current = fromJson(rec.getString("stemVolumes"),
                new TypeReference<>() {});
        current.putAll(newVolumes);

        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, key, new Bin("stemVolumes", toJson(current)));

        // Accumulate royalty: 1 minute credited per mix update event (approximation)
        if (!participantId.toString().equals(hostUserId)) {
            accumulateRoyalty(sessionId, hostUserId, participantId.toString(), 1.0);
        }

        return current;
    }

    /**
     * Returns the current session state or null if not found.
     */
    public Map<String, Object> getSession(UUID sessionId) {
        Key key = new Key(NS, SESSION_SET, sessionId.toString());
        Record rec = aerospike.get(null, key);
        if (rec == null) return null;

        return Map.of(
                "sessionId",  sessionId,
                "hostUserId", rec.getString("hostUserId"),
                "trackId",    rec.getString("trackId"),
                "participants", fromJson(rec.getString("participants"),
                        new TypeReference<List<String>>() {}),
                "stemVolumes", fromJson(rec.getString("stemVolumes"),
                        new TypeReference<Map<String, Double>>() {}),
                "startedAt",  rec.getLong("startedAt")
        );
    }

    // -------------------------------------------------------------------------
    // Micro-royalty accumulation + hourly flush
    // -------------------------------------------------------------------------

    private void accumulateRoyalty(UUID sessionId, String hostUserId,
                                    String listenerUserId, double minutes) {
        Key key = new Key(NS, ROYALTY_SET,
                sessionId.toString() + ":" + listenerUserId);

        Record existing = aerospike.get(null, key, "listenerMinutes");
        double total = (existing != null ? existing.getDouble("listenerMinutes") : 0.0) + minutes;

        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, key,
                new Bin("listenerMinutes", total),
                new Bin("hostUserId",      hostUserId),
                new Bin("listenerUserId",  listenerUserId),
                new Bin("sessionId",       sessionId.toString()));
    }

    /**
     * Hourly job: scans pending royalty records in Aerospike and flushes them
     * to the stem_royalty_ledger PostgreSQL table.
     *
     * In production, replace the in-memory tracking below with an Aerospike
     * scan policy that iterates over the stem_session_royalties set.
     */
    @Scheduled(fixedRateString = "${stem.royalty.flush.interval.ms:3600000}")
    public void flushRoyaltiesToPostgres() {
        log.info("Starting hourly royalty flush to PostgreSQL");
        try {
            List<Map.Entry<Key, Record>> pending = new ArrayList<>();

            ScanPolicy sp = new ScanPolicy();
            aerospike.scanAll(sp, NS, ROYALTY_SET, (key, record) -> {
                if (record != null) {
                    pending.add(Map.entry(key, record));
                }
            });

            if (pending.isEmpty()) {
                log.info("Royalty flush: no pending records found");
                return;
            }

            List<StemRoyaltyLedger> ledgerEntries = new ArrayList<>(pending.size());
            for (Map.Entry<Key, Record> entry : pending) {
                Record rec = entry.getValue();
                try {
                    UUID sessionId      = UUID.fromString(rec.getString("sessionId"));
                    UUID hostUserId     = UUID.fromString(rec.getString("hostUserId"));
                    UUID listenerUserId = UUID.fromString(rec.getString("listenerUserId"));
                    double minutes      = rec.getDouble("listenerMinutes");

                    BigDecimal listenerMinutes = BigDecimal.valueOf(minutes);
                    BigDecimal royaltyAmount   = BigDecimal.valueOf(minutes * ROYALTY_PER_LISTENER_MINUTE);

                    ledgerEntries.add(new StemRoyaltyLedger(
                        sessionId, hostUserId, listenerUserId, listenerMinutes, royaltyAmount));
                } catch (Exception e) {
                    log.warn("Skipping malformed royalty record key={}: {}", entry.getKey(), e.getMessage());
                }
            }

            royaltyRepo.saveAll(ledgerEntries);
            log.info("Royalty flush: persisted {} records to PostgreSQL", ledgerEntries.size());

            // Delete flushed records from Aerospike (at-least-once: only after successful save)
            for (Map.Entry<Key, Record> entry : pending) {
                try {
                    aerospike.delete(null, entry.getKey());
                } catch (Exception e) {
                    log.warn("Failed to delete flushed royalty record key={}: {}", entry.getKey(), e.getMessage());
                }
            }

            log.info("Royalty flush: removed {} records from Aerospike", pending.size());

        } catch (Exception e) {
            log.error("Royalty flush failed — records left intact in Aerospike for next run: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try { return objectMapper.readValue(json, type); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
