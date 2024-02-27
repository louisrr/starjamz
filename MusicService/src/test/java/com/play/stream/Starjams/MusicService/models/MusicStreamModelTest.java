package com.play.stream.Starjams.MusicService.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MusicStreamModelTest {

    private MusicStreamModel musicStreamModel;
    private final UUID streamId = UUID.randomUUID();
    private final UUID trackId = UUID.randomUUID();
    private final String trackUrl = "http://example.com/track";
    private final String title = "Test Track";
    private final String artist = "Test Artist";
    private final String album = "Test Album";
    private final String genre = "Test Genre";
    private final Long duration = 300L;
    private final String streamUrl = "http://example.com/stream";
    private final String coverArtUrl = "http://example.com/cover";
    private final LocalDate releaseDate = LocalDate.of(2020, 1, 1);
    private final String quality = "High";
    private final List<String> tags = Arrays.asList("Tag1", "Tag2");
    private final String license = "Test License";

    @BeforeEach
    void setUp() {
        musicStreamModel = new MusicStreamModel();
        musicStreamModel.setStreamId(streamId);
        musicStreamModel.setTrackId(trackId);
        musicStreamModel.setTrackUrl(trackUrl);
        musicStreamModel.setTitle(title);
        musicStreamModel.setArtist(artist);
        musicStreamModel.setAlbum(album);
        musicStreamModel.setGenre(genre);
        musicStreamModel.setDuration(duration);
        musicStreamModel.setStreamUrl(streamUrl);
        musicStreamModel.setCoverArtUrl(coverArtUrl);
        musicStreamModel.setReleaseDate(releaseDate);
        musicStreamModel.setQuality(quality);
        musicStreamModel.setTags(tags);
        musicStreamModel.setLicense(license);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(streamId, musicStreamModel.getStreamId());
        assertEquals(trackId, musicStreamModel.getTrackId());
        assertEquals(trackUrl, musicStreamModel.getTrackUrl());
        assertEquals(title, musicStreamModel.getTitle());
        assertEquals(artist, musicStreamModel.getArtist());
        assertEquals(album, musicStreamModel.getAlbum());
        assertEquals(genre, musicStreamModel.getGenre());
        assertEquals(duration, musicStreamModel.getDuration());
        assertEquals(streamUrl, musicStreamModel.getStreamUrl());
        assertEquals(coverArtUrl, musicStreamModel.getCoverArtUrl());
        assertEquals(releaseDate, musicStreamModel.getReleaseDate());
        assertEquals(quality, musicStreamModel.getQuality());
        assertEquals(tags, musicStreamModel.getTags());
        assertEquals(license, musicStreamModel.getLicense());
    }
}
