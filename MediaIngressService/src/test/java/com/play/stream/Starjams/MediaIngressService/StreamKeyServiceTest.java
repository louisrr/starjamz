package com.play.stream.Starjams.MediaIngressService;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.play.stream.Starjams.MediaIngressService.dto.StreamKeyRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.services.StreamKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamKeyServiceTest {

    @Mock
    private IAerospikeClient aerospike;

    private StreamKeyService streamKeyService;

    @BeforeEach
    void setUp() {
        streamKeyService = new StreamKeyService(aerospike);
        ReflectionTestUtils.setField(streamKeyService, "streamKeyTtlHours", 6);
    }

    @Test
    void generateStreamKey_returnsNonNullKey() {
        UUID userId = UUID.randomUUID();
        String key = streamKeyService.generateStreamKey(userId, StreamPlatform.MOBILE_IOS);

        assertThat(key).isNotNull().hasSize(32);
        verify(aerospike).put(any(), any(), any());
    }

    @Test
    void generateStreamKey_keysAreUnique() {
        UUID userId = UUID.randomUUID();
        String key1 = streamKeyService.generateStreamKey(userId, StreamPlatform.MOBILE_IOS);
        String key2 = streamKeyService.generateStreamKey(userId, StreamPlatform.MOBILE_ANDROID);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void validateStreamKey_returnsRecord_whenKeyExists() {
        UUID userId = UUID.randomUUID();
        String streamKey = "abc123";
        long now = System.currentTimeMillis();

        Map<String, Object> bins = new HashMap<>();
        bins.put("userId",    userId.toString());
        bins.put("platform",  "MOBILE_IOS");
        bins.put("createdAt", now);
        bins.put("expiresAt", now + 6 * 3600 * 1000L);

        Record mockRecord = new Record(bins, 1, 21600);
        when(aerospike.get(any(), any(Key.class))).thenReturn(mockRecord);

        StreamKeyRecord rec = streamKeyService.validateStreamKey(streamKey);

        assertThat(rec).isNotNull();
        assertThat(rec.getUserId()).isEqualTo(userId);
        assertThat(rec.getPlatform()).isEqualTo(StreamPlatform.MOBILE_IOS);
    }

    @Test
    void validateStreamKey_throwsException_whenKeyNotFound() {
        when(aerospike.get(any(), any(Key.class))).thenReturn(null);

        assertThatThrownBy(() -> streamKeyService.validateStreamKey("expired-key"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void revokeStreamKey_callsDelete() {
        streamKeyService.revokeStreamKey("someKey");
        verify(aerospike).delete(any(), any(Key.class));
    }

    @Test
    void isOwner_returnsTrue_whenUserOwnsKey() {
        UUID userId = UUID.randomUUID();
        String streamKey = "testKey";
        long now = System.currentTimeMillis();

        Map<String, Object> bins = new HashMap<>();
        bins.put("userId",    userId.toString());
        bins.put("platform",  "MOBILE_ANDROID");
        bins.put("createdAt", now);
        bins.put("expiresAt", now + 6 * 3600 * 1000L);

        when(aerospike.get(any(), any(Key.class))).thenReturn(new Record(bins, 1, 21600));

        assertThat(streamKeyService.isOwner(streamKey, userId)).isTrue();
        assertThat(streamKeyService.isOwner(streamKey, UUID.randomUUID())).isFalse();
    }
}
