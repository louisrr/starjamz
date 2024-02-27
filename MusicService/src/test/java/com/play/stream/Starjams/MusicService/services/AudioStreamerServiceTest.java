package com.play.stream.Starjams.MusicService.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.freedesktop.gstreamer.Pipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AudioStreamerServiceTest {

    @Mock
    private AmazonS3 mockS3Client;
    @Mock
    private CqlSession mockCqlSession;
    @Mock
    private Pipeline mockPipeline;
    @Mock
    private ResultSet mockResultSet;

    private AudioStreamerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Assume Gst.init and Gst.parseLaunch are managed and focus on the mockable parts
        when(mockCqlSession.execute(any(String.class))).thenReturn(mockResultSet);
        // Mock the pipeline creation process to return a mocked pipeline
        System.setProperty("org.freedesktop.gstreamer.no_init", "true"); // Prevent Gst.init
        service = new AudioStreamerService("testSourceUrl", true, mockCqlSession) {
            @Override
            protected void setupPipeline(String sourceUrl) {
                this.pipeline = mockPipeline; // Use the mocked pipeline
            }
        };
        service.s3client = mockS3Client; // Inject the mocked S3 client
    }

    @Test
    void testStart() {
        service.start();
        verify(mockPipeline, times(1)).play();
    }

    @Test
    void testStopAndSaveStream() {
        // Configure service to save stream to S3 and ScyllaDB
        service.outputFilePath = "local/path/to/save/test.mp4"; // Set for test predictability

        service.stop();

        // Verify pipeline was stopped
        verify(mockPipeline, times(1)).stop();

        // Verify file was uploaded to S3
        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class));

        // Verify ScyllaDB was called to save stream info
        verify(mockCqlSession, atLeastOnce()).execute(any(String.class));
    }
}
