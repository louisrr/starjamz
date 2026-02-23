package com.play.stream.Starjams.GatewayService.controllers;

import com.play.stream.Starjams.GatewayService.services.GatewayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/gateway/playlists")
public class PlaylistGatewayController {

    private final GatewayService gatewayService;

    public PlaylistGatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @GetMapping
    public ResponseEntity<String> getAllPlaylists() {
        return ResponseEntity.ok(gatewayService.getAllPlaylists());
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<String> getPlaylistById(@PathVariable String playlistId) {
        return ResponseEntity.ok(gatewayService.getPlaylistById(playlistId));
    }

    @PostMapping
    public ResponseEntity<String> createPlaylist(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gatewayService.createPlaylist(payload));
    }
}
