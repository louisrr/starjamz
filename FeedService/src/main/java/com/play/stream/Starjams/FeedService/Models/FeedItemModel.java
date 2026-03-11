package com.play.stream.Starjams.FeedService.Models;

import java.util.UUID;

/** @deprecated Replaced by {@link com.play.stream.Starjams.FeedService.model.FeedEvent}. */
@Deprecated
public class FeedItemModel {
    private UUID id;
    private String title;
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
