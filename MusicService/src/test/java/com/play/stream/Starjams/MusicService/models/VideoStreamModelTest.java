package com.play.stream.Starjams.MusicService.models;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class VideoStreamModelTest {

    @Test
    public void testVideoStreamModelSettersAndGetters() {
        UUID streamId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        String videoUrl = "http://example.com/video";
        String title = "Test Video";
        String director = "Jane Doe";
        String productionHouse = "Example Productions";
        String genre = "Drama";
        Long duration = 3600L; // 1 hour in seconds
        String streamUrl = "http://example.com/stream";
        String thumbnailUrl = "http://example.com/thumbnail";
        LocalDate releaseDate = LocalDate.of(2024, 2, 27);
        String quality = "1080p";
        List<String> tags = Arrays.asList("drama", "test");
        String license = "Creative Commons";
        List<String> cast = Arrays.asList("Actor 1", "Actor 2");
        String language = "English";

        VideoStream model = new VideoStream();
        model.setStreamId(streamId);
        model.setVideoId(videoId);
        model.setVideoUrl(videoUrl);
        model.setTitle(title);
        model.setDirector(director);
        model.setProductionHouse(productionHouse);
        model.setGenre(genre);
        model.setDuration(duration);
        model.setStreamUrl(streamUrl);
        model.setThumbnailUrl(thumbnailUrl);
        model.setReleaseDate(releaseDate);
        model.setQuality(quality);
        model.setTags(tags);
        model.setLicense(license);
        model.setCast(cast);
        model.setLanguage(language);

        assertEquals(streamId, model.getStreamId());
        assertEquals(videoId, model.getVideoId());
        assertEquals(videoUrl, model.getVideoUrl());
        assertEquals(title, model.getTitle());
        assertEquals(director, model.getDirector());
        assertEquals(productionHouse, model.getProductionHouse());
        assertEquals(genre, model.getGenre());
        assertEquals(duration, model.getDuration());
        assertEquals(streamUrl, model.getStreamUrl());
        assertEquals(thumbnailUrl, model.getThumbnailUrl());
        assertEquals(releaseDate, model.getReleaseDate());
        assertEquals(quality, model.getQuality());
        assertEquals(tags, model.getTags());
        assertEquals(license, model.getLicense());
        assertEquals(cast, model.getCast());
        assertEquals(language, model.getLanguage());
    }

    @Test
    public void testVideoStreamModelEquality() {
        VideoStream model1 = new VideoStream();
        model1.setStreamId(UUID.randomUUID());
        model1.setTitle("Test Video");

        VideoStream model2 = new VideoStream();
        model2.setStreamId(model1.getStreamId());
        model2.setTitle(model1.getTitle());

        assertEquals(model1, model2);
        assertEquals(model1.hashCode(), model2.hashCode());

        model2.setStreamId(UUID.randomUUID()); // Change ID
        assertNotEquals(model1, model2);
    }

    @Test
    public void testVideoStreamModelToString() {
        VideoStream model = new VideoStream();
        model.setTitle("Test Video");
        String toStringResult = model.toString();
        assert(toStringResult.contains("Test Video")); // Simple check to ensure toString includes part of the state
    }
}
