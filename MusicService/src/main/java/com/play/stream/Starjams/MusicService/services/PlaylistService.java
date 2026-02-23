package com.play.stream.Starjams.MusicService.services;

import com.play.stream.Starjams.MusicService.models.Playlist;
import com.play.stream.Starjams.MusicService.repository.PlaylistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    public List<Playlist> getPlayListByUserId(String userId) {
        return playlistRepository.findByUserId(userId);
    }

    public List<Playlist> getPlayListById(String playlistId) {
        return playlistRepository.findByUserId(playlistId);
    }

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
