package com.play.stream.Starjams.PlaylistService.dto;

import jakarta.validation.constraints.NotBlank;

public class AddTrackRequest {

    @NotBlank
    private String trackId;

    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
}
