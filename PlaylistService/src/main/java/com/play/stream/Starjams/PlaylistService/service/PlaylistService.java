package com.play.stream.Starjams.PlaylistService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.PlaylistService.config.KafkaTopics;
import com.play.stream.Starjams.PlaylistService.dto.AddTrackRequest;
import com.play.stream.Starjams.PlaylistService.dto.CreatePlaylistRequest;
import com.play.stream.Starjams.PlaylistService.dto.PlaylistResponse;
import com.play.stream.Starjams.PlaylistService.model.Playlist;
import com.play.stream.Starjams.PlaylistService.repository.PlaylistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private final PlaylistRepository playlistRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public PlaylistService(PlaylistRepository playlistRepo,
                           KafkaTemplate<String, String> kafka,
                           ObjectMapper objectMapper) {
        this.playlistRepo = playlistRepo;
        this.kafka        = kafka;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlaylistResponse createPlaylist(String ownerId, CreatePlaylistRequest req) {
        Playlist playlist = new Playlist();
        playlist.setOwnerId(ownerId);
        playlist.setTitle(req.getTitle());
        playlist.setDescription(req.getDescription());
        playlist.setPublic(req.isPublic());
        Instant now = Instant.now();
        playlist.setCreatedAt(now);
        playlist.setUpdatedAt(now);

        Playlist saved = playlistRepo.save(playlist);
        publishEvent(saved.getId(), ownerId, "PLAYLIST_CREATED");
        log.info("Created playlist {} for owner {}", saved.getId(), ownerId);
        return PlaylistResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylist(UUID id) {
        return playlistRepo.findById(id)
            .map(PlaylistResponse::from)
            .orElseThrow(() -> new NoSuchElementException("Playlist not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> getUserPlaylists(String ownerId) {
        return playlistRepo.findByOwnerIdOrderByCreatedAtDesc(ownerId)
            .stream()
            .map(PlaylistResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public PlaylistResponse addTrack(UUID playlistId, AddTrackRequest req) {
        Playlist playlist = findOrThrow(playlistId);
        if (!playlist.getTrackIds().contains(req.getTrackId())) {
            playlist.getTrackIds().add(req.getTrackId());
        }
        playlist.setUpdatedAt(Instant.now());
        Playlist saved = playlistRepo.save(playlist);
        publishEvent(saved.getId(), saved.getOwnerId(), "TRACK_ADDED");
        return PlaylistResponse.from(saved);
    }

    @Transactional
    public PlaylistResponse removeTrack(UUID playlistId, String trackId) {
        Playlist playlist = findOrThrow(playlistId);
        playlist.getTrackIds().remove(trackId);
        playlist.setUpdatedAt(Instant.now());
        Playlist saved = playlistRepo.save(playlist);
        publishEvent(saved.getId(), saved.getOwnerId(), "TRACK_REMOVED");
        return PlaylistResponse.from(saved);
    }

    @Transactional
    public void deletePlaylist(UUID playlistId) {
        Playlist playlist = findOrThrow(playlistId);
        publishEvent(playlist.getId(), playlist.getOwnerId(), "PLAYLIST_DELETED");
        playlistRepo.deleteById(playlistId);
        log.info("Deleted playlist {}", playlistId);
    }

    // -------------------------------------------------------------------------

    private Playlist findOrThrow(UUID id) {
        return playlistRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Playlist not found: " + id));
    }

    private void publishEvent(UUID playlistId, String ownerId, String eventType) {
        try {
            Map<String, String> payload = Map.of(
                "playlistId", playlistId.toString(),
                "ownerId",    ownerId,
                "eventType",  eventType
            );
            String json = objectMapper.writeValueAsString(payload);
            kafka.send(KafkaTopics.PLAYLIST_EVENT, playlistId.toString(), json);
        } catch (Exception e) {
            log.warn("Failed to publish playlist.event for playlist {}: {}", playlistId, e.getMessage());
        }
    }
}
