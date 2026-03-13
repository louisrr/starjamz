package com.play.stream.Starjams.MediaIngressService;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.play.stream.Starjams.MediaIngressService.dto.LiveSessionDto;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;
import com.play.stream.Starjams.MediaIngressService.services.LiveSessionRegistry;
import com.play.stream.Starjams.MediaIngressService.services.StreamKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests the stream key → session lifecycle without a live Aerospike instance.
 */
@ExtendWith(MockitoExtension.class)
class StreamLifecycleIntegrationTest {

    @Mock
    private IAerospikeClient aerospike;

    private StreamKeyService streamKeyService;
    private LiveSessionRegistry sessionRegistry;

    @BeforeEach
    void setUp() {
        streamKeyService = new StreamKeyService(aerospike);
        ReflectionTestUtils.setField(streamKeyService, "streamKeyTtlHours", 6);

        sessionRegistry = new LiveSessionRegistry(aerospike);
    }

    @Test
    void streamKey_isIssuedAndLinkedToUser() {
        UUID userId = UUID.randomUUID();
        String streamKey = streamKeyService.generateStreamKey(userId, StreamPlatform.YOUTUBE);

        assertThat(streamKey).isNotBlank().hasSize(32);
        verify(aerospike, times(1)).put(any(), any(Key.class), any());
    }

    @Test
    void sessionRegistry_createSession_writesToAerospike() {
        String streamKey = "abc123";
        UUID userId = UUID.randomUUID();

        sessionRegistry.createSession(streamKey, userId, StreamPlatform.TWITCH);

        verify(aerospike, times(1)).put(any(), any(Key.class), any());
    }

    @Test
    void sessionRegistry_setStatus_updatesAerospike() {
        String streamKey = "abc123";
        sessionRegistry.setStatus(streamKey, StreamStatus.ENDED);

        verify(aerospike, times(1)).put(any(), any(Key.class), any());
    }

    @Test
    void sessionRegistry_getSession_mapsRecordCorrectly() {
        String streamKey = "abc123";
        UUID userId = UUID.randomUUID();
        long now = Instant.now().toEpochMilli();

        Map<String, Object> bins = new HashMap<>();
        bins.put("userId",      userId.toString());
        bins.put("platform",    "MOBILE_IOS");
        bins.put("status",      "LIVE");
        bins.put("viewerCount", 7L);
        bins.put("startedAt",   now);
        bins.put("hlsManifestUrl", "https://cdn.starjamz.com/hls/" + streamKey + "/playlist.m3u8");

        when(aerospike.get(any(), any(Key.class))).thenReturn(new Record(bins, 1, 0));

        LiveSessionDto dto = sessionRegistry.getSession(streamKey);

        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getPlatform()).isEqualTo(StreamPlatform.MOBILE_IOS);
        assertThat(dto.getStatus()).isEqualTo(StreamStatus.LIVE);
        assertThat(dto.getViewerCount()).isEqualTo(7L);
        assertThat(dto.getHlsManifestUrl()).contains(streamKey);
    }

    @Test
    void sessionRegistry_getSession_returnsNull_whenNotFound() {
        when(aerospike.get(any(), any(Key.class))).thenReturn(null);
        assertThat(sessionRegistry.getSession("unknownKey")).isNull();
    }
}
