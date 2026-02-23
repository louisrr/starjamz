package com.play.stream.Starjams.MusicService.models;

import com.google.common.base.Objects;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class VideoStream {

    @PrimaryKey
    private UUID streamId; // Unique identifier for the video stream

    @Column
    private UUID videoId; // Unique identifier for the video, analogous to trackId

    @Column
    private String videoUrl; // URL to access the video, similar to trackUrl

    @Column
    private String title; // Title of the video

    @Column
    private String director; // Director of the video, analogous to artist

    @Column
    private String productionHouse; // Production house, analogous to album

    @Column
    private String genre; // Genre of the video

    @Column
    private Long duration; // Duration of the video in seconds

    @Column
    private String streamUrl; // URL to stream the video, similar to streamUrl for music

    @Column
    private String thumbnailUrl; // URL for the video's thumbnail, analogous to coverArtUrl

    @Column
    private LocalDate releaseDate; // Release date of the video

    @Column
    private String quality; // Quality of the video stream (e.g., 1080p, 4K)

    @Column
    private List<String> tags; // Tags related to the video for search and categorization

    @Column
    private String license; // Licensing information for the video

    @Column
    private List<String> cast; // List of main actors or people featured in the video

    @Column
    private String language; // Primary language of the video

    // Getter and Setter methods for each field

    public UUID getStreamId() {
        return streamId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDirector() {
        return director;
    }

    public String getProductionHouse() {
        return productionHouse;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
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

    public List<String> getCast() {
        return cast;
    }

    public String getLanguage() {
        return language;
    }

    public void setStreamId(UUID streamId) {
        this.streamId = streamId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public void setProductionHouse(String productionHouse) {
        this.productionHouse = productionHouse;
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

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

    public void setCast(List<String> cast) {
        this.cast = cast;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoStream)) return false;
        VideoStream that = (VideoStream) o;
        return Objects.equal(getStreamId(), that.getStreamId()) && Objects.equal(getVideoId(), that.getVideoId()) && Objects.equal(getVideoUrl(), that.getVideoUrl()) && Objects.equal(getTitle(), that.getTitle()) && Objects.equal(getDirector(), that.getDirector()) && Objects.equal(getProductionHouse(), that.getProductionHouse()) && Objects.equal(getGenre(), that.getGenre()) && Objects.equal(getDuration(), that.getDuration()) && Objects.equal(getStreamUrl(), that.getStreamUrl()) && Objects.equal(getThumbnailUrl(), that.getThumbnailUrl()) && Objects.equal(getReleaseDate(), that.getReleaseDate()) && Objects.equal(getQuality(), that.getQuality()) && Objects.equal(getTags(), that.getTags()) && Objects.equal(getLicense(), that.getLicense()) && Objects.equal(getCast(), that.getCast()) && Objects.equal(getLanguage(), that.getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getStreamId(), getVideoId(), getVideoUrl(), getTitle(), getDirector(), getProductionHouse(), getGenre(), getDuration(), getStreamUrl(), getThumbnailUrl(), getReleaseDate(), getQuality(), getTags(), getLicense(), getCast(), getLanguage());
    }

    @Override
    public String toString() {
        return "VideoStream{" +
                "streamId=" + streamId +
                ", videoId=" + videoId +
                ", videoUrl='" + videoUrl + '\'' +
                ", title='" + title + '\'' +
                ", director='" + director + '\'' +
                ", productionHouse='" + productionHouse + '\'' +
                ", genre='" + genre + '\'' +
                ", duration=" + duration +
                ", streamUrl='" + streamUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", releaseDate=" + releaseDate +
                ", quality='" + quality + '\'' +
                ", tags=" + tags +
                ", license='" + license + '\'' +
                ", cast=" + cast +
                ", language='" + language + '\'' +
                '}';
    }


}
