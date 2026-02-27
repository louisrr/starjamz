package com.play.Starjams.MediaService.services;

import com.play.Starjams.MediaService.models.StreamSession;
import com.play.Starjams.MediaService.models.StreamStatus;
import org.freedesktop.gstreamer.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of active {@link StreamSession} objects.
 *
 * Handles session registration, lookup, graceful teardown, and periodic
 * eviction of sessions that have finished or outlived their timeout.
 */
@Service
public class StreamSessionManager {

    private static final Logger log = LoggerFactory.getLogger(StreamSessionManager.class);

    @Value("${stream.session.timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    private final Map<String, StreamSession> sessions = new ConcurrentHashMap<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public String newSessionId() {
        return UUID.randomUUID().toString();
    }

    public void register(StreamSession session) {
        sessions.put(session.getId(), session);
        log.info("Registered session {} ({})", session.getId(), session.getType());
    }

    public Optional<StreamSession> get(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public Collection<StreamSession> all() {
        return sessions.values();
    }

    /**
     * Stops the pipeline for the given session, cleans up disk resources, and
     * removes it from the registry.
     */
    public void stop(String id) {
        StreamSession session = sessions.remove(id);
        if (session != null) {
            teardown(session);
        }
    }

    // ── Scheduled maintenance ─────────────────────────────────────────────────

    /**
     * Evicts sessions that have ended (STOPPED / ERROR) or that have been
     * running longer than the configured timeout without an active HTTP client.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60_000)
    public void evictStaleSessions() {
        Instant cutoff = Instant.now().minus(sessionTimeoutMinutes, ChronoUnit.MINUTES);
        List<String> stale = sessions.values().stream()
                .filter(s -> !s.isActive() || s.getStartedAt().isBefore(cutoff))
                .map(StreamSession::getId)
                .toList();

        if (!stale.isEmpty()) {
            log.info("Evicting {} stale session(s): {}", stale.size(), stale);
            stale.forEach(this::stop);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void teardown(StreamSession session) {
        session.setStatus(StreamStatus.STOPPED);

        if (session.getPipeline() != null) {
            try {
                session.getPipeline().setState(State.NULL);
            } catch (Exception e) {
                log.warn("[{}] Error while stopping pipeline: {}", session.getId(), e.getMessage());
            }
        }

        // Remove HLS segment directory for LIVE_VIDEO sessions
        if (session.getHlsOutputDir() != null) {
            deleteDirectory(new File(session.getHlsOutputDir()));
        }

        log.info("Stopped and removed session {}", session.getId());
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) child.delete();
        }
        dir.delete();
    }
}
