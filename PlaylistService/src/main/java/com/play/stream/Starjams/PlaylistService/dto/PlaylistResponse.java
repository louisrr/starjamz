package com.play.stream.Starjams.PlaylistService.dto;

import com.play.stream.Starjams.PlaylistService.model.Playlist;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PlaylistResponse {

    private UUID id;
    private String ownerId;
    private String title;
    private String description;
    private List<String> trackIds;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isPublic;

    public static PlaylistResponse from(Playlist p) {
        PlaylistResponse r = new PlaylistResponse();
        r.id          = p.getId();
        r.ownerId     = p.getOwnerId();
        r.title       = p.getTitle();
        r.description = p.getDescription();
        r.trackIds    = p.getTrackIds();
        r.createdAt   = p.getCreatedAt();
        r.updatedAt   = p.getUpdatedAt();
        r.isPublic    = p.isPublic();
        return r;
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getTrackIds() { return trackIds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isPublic() { return isPublic; }
}
