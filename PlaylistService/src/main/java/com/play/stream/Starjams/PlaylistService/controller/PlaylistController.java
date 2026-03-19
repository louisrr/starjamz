package com.play.stream.Starjams.PlaylistService.controller;

import com.play.stream.Starjams.PlaylistService.dto.AddTrackRequest;
import com.play.stream.Starjams.PlaylistService.dto.CreatePlaylistRequest;
import com.play.stream.Starjams.PlaylistService.dto.PlaylistResponse;
import com.play.stream.Starjams.PlaylistService.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> createPlaylist(
            @RequestParam String ownerId,
            @Valid @RequestBody CreatePlaylistRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(playlistService.createPlaylist(ownerId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable UUID id) {
        return ResponseEntity.ok(playlistService.getPlaylist(id));
    }

    @GetMapping("/user/{ownerId}")
    public ResponseEntity<List<PlaylistResponse>> getUserPlaylists(@PathVariable String ownerId) {
        return ResponseEntity.ok(playlistService.getUserPlaylists(ownerId));
    }

    @PostMapping("/{id}/tracks")
    public ResponseEntity<PlaylistResponse> addTrack(
            @PathVariable UUID id,
            @Valid @RequestBody AddTrackRequest req) {
        return ResponseEntity.ok(playlistService.addTrack(id, req));
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<PlaylistResponse> removeTrack(
            @PathVariable UUID id,
            @PathVariable String trackId) {
        return ResponseEntity.ok(playlistService.removeTrack(id, trackId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.noContent().build();
    }
}
