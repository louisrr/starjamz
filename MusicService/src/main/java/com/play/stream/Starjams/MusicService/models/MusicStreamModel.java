package com.play.stream.Starjams.MusicService.models;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class MusicStreamModel {

    @PrimaryKey
    private UUID streamId;

    @Column
    private UUID trackId;

    @Column
    private String trackUrl;

    @Column
    private String title;

    @Column
    private String artist;

    @Column
    private String album;

    @Column
    private String genre;

    @Column
    private Long duration;

    @Column
    private String streamUrl;

    @Column
    private String coverArtUrl;

    @Column
    private LocalDate releaseDate;

    @Column
    private String quality;

    @Column
    private List<String> tags;

    @Column
    private String license;

    public UUID getStreamId() {
        return streamId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public String getTrackUrl() {
        return trackUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public Long getDuration() {
        return duration;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getQuality() {
        return quality;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getLicense() {
        return license;
    }

    public void setStreamId(UUID streamId) {
        this.streamId = streamId;
    }

    public void setTrackId(UUID trackId) {
        this.trackId = trackId;
    }

    public void setTrackUrl(String trackUrl) {
        this.trackUrl = trackUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public void setCoverArtUrl(String coverArtUrl) {
        this.coverArtUrl = coverArtUrl;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}