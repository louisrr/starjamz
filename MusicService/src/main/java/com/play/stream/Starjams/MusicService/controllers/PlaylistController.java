package com.play.stream.Starjams.MusicService.controllers;

import com.play.stream.Starjams.MusicService.models.Playlist;
import com.play.stream.Starjams.MusicService.repository.PlaylistRepository;
import com.play.stream.Starjams.MusicService.services.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private PlaylistRepository playlistRepository;

    @GetMapping("/api/playlists/{userId}")
    public ResponseEntity<List<Playlist>> getPlaylistsByUserId(@PathVariable String userId) {
        List<Playlist> playlists = playlistService.getPlayListByUserId(userId);
        if (playlists.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(playlists);
    }

    @GetMapping("/api/playlists/{userId}/{playlistId}")
    public ResponseEntity<List<Playlist>> getPlaylistsId(@PathVariable String playlistId) {
        List<Playlist> playlists = playlistService.getPlayListById(playlistId);
        if (playlists.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(playlists);
    }

    @DeleteMapping("/api/playlists/{playlistId}")
    public boolean deletePlaylistById(String playlistId) {
        // Assuming you use a repository to interact with your database
        try {
            playlistRepository.deleteById(playlistId);
            return true;  // Return true if deletion is successful
        } catch (Exception e) {
            return false; // Return false if deletion fails
        }
    }

}
