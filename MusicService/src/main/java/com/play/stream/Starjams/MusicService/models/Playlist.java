package com.play.stream.Starjams.MusicService.models;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;

@Table("content_entry")
public class Playlist {

    @PrimaryKey
    private String id;
    private String userId;
    private String name;
    private List<String> tracks; // Assuming tracks are stored as a list of track IDs

    // Constructors, getters, and setters

    public Playlist() {}

    public Playlist(String id, String userId, String name, List<String> tracks) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.tracks = tracks;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTracks() {
        return tracks;
    }

    public void setTracks(List<String> tracks) {
        this.tracks = tracks;
    }
}
