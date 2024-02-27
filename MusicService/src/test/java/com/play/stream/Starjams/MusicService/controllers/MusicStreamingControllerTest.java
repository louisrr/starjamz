package com.play.stream.Starjams.MusicService.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.containsString;

import com.play.stream.Starjams.MusicService.services.MusicStreamingService;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(MusicStreamingController.class)
public class MusicStreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MusicStreamingService musicStreamingService;

    private Resource mockResource;

    @BeforeEach
    void setUp() throws Exception {
        mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.getFilename()).thenReturn("test.mp3");
        Mockito.when(mockResource.exists()).thenReturn(true);
    }

    @Test
    public void streamAudio_ReturnsFile_WhenFileExists() throws Exception {
        given(musicStreamingService.loadAudioAsResource("test.mp3")).willReturn(mockResource);

        mockMvc.perform(MockMvcRequestBuilders.get("/stream/test.mp3"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"test.mp3\"")))
                .andExpect(content().contentType(MediaType.parseMediaType("audio/mpeg")));
    }

    @Test
    public void streamAudio_ReturnsNotFound_WhenFileDoesNotExist() throws Exception {
        Mockito.when(musicStreamingService.loadAudioAsResource("nonexistent.mp3")).thenThrow(new RuntimeException("File not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/stream/nonexistent.mp3"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                .andExpect(result -> assertEquals("File not found", result.getResolvedException().getMessage()));
    }
}
