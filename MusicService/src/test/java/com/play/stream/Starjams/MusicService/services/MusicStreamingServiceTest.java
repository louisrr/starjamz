package com.play.stream.Starjams.MusicService.services;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MusicStreamingServiceTest {

    @Mock
    private CqlSession mockSession;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private Row mockRow;

    private MusicStreamingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MusicStreamingService(mockSession, "src/test/resources");
    }

    @Test
    void loadAudioAsResource_FileExistsAndReadable() throws Exception {
        when(mockSession.execute(anyString(), anyString())).thenReturn(mockResultSet);
        when(mockResultSet.one()).thenReturn(mockRow);
        when(mockRow.getString("file_path")).thenReturn("test-audio.mp3");

        Resource resource = service.loadAudioAsResource("test-audio.mp3");

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadAudioAsResource_FileNotFoundInDatabase() {
        when(mockSession.execute(anyString(), anyString())).thenReturn(mockResultSet);
        when(mockResultSet.one()).thenReturn(null);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.loadAudioAsResource("nonexistent.mp3"));
        assertTrue(thrown.getMessage().contains("File not found"));
    }

    @Test
    void loadAudioAsResource_FilePathMalformed() {
        when(mockSession.execute(anyString(), anyString())).thenReturn(mockResultSet);
        when(mockResultSet.one()).thenReturn(mockRow);
        // Provide an invalid file path to simulate a MalformedURLException
        when(mockRow.getString("file_path")).thenReturn("///\\invalid-path");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.loadAudioAsResource("invalid.mp3"));
        assertTrue(thrown.getMessage().contains("Error"));
    }
}
