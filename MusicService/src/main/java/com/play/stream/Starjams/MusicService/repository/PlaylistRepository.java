package com.play.stream.Starjams.MusicService.repository;

import com.play.stream.Starjams.MusicService.models.Playlist;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import com.play.stream.Starjams.MusicService.services.PlaylistService;
import java.util.List;

@Repository
public interface PlaylistRepository extends CassandraRepository<PlaylistService, String> {
    List<Playlist> findByUserId(String userId);
}

