package com.stream.Starjams.GatewayService.controllers;

import com.stream.Starjams.GatewayService.datatransferobjects.ItemDTO;
import com.stream.Starjams.GatewayService.client.MusicServiceClient;
import com.stream.Starjams.GatewayService.datatransferobjects.PlaylistDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/gateway/playlists")
public class PlaylistGatewayController {

    @Autowired
    private MusicServiceClient musicServiceClient;

    @GetMapping("/user-playlists/{userId}")
    public Flux<ResponseEntity<PlaylistDTO>> getPlaylistsByUserId(@PathVariable String userId) {
        return musicServiceClient.getPlaylistsByUserId(userId)
                .map(playlist -> ResponseEntity.ok(playlist))
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @PostMapping("/{playlistId}/items")
    public Mono<ResponseEntity<PlaylistDTO>> addItemToPlaylist(@PathVariable Long playlistId, @RequestBody ItemDTO item) {
        return musicServiceClient.addItemToPlaylist(playlistId, item);
    }
}