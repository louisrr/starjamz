package com.play.stream.Starjams.PlaylistService.repository;

import com.play.stream.Starjams.PlaylistService.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    List<Playlist> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
