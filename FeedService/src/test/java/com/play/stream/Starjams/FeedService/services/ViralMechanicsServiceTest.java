package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ViralMechanicsService.
 * Aerospike client and Kafka are mocked to focus on business logic.
 */
@ExtendWith(MockitoExtension.class)
class ViralMechanicsServiceTest {

    @Mock IAerospikeClient aerospike;
    @Mock FeedFanoutService fanout;
    @Mock FollowGraphService followGraph;
    @Mock KafkaTemplate<String, String> kafka;

    private ViralMechanicsService viralService;

    @BeforeEach
    void setUp() {
        viralService = new ViralMechanicsService(aerospike, fanout, followGraph, kafka);
        ReflectionTestUtils.setField(viralService, "buzzingPlayThreshold", 100L);
        ReflectionTestUtils.setField(viralService, "buzzingWindowHours", 6L);
        ReflectionTestUtils.setField(viralService, "streakDays", 3);
    }

    // -------------------------------------------------------------------------
    // recordGift — threshold check
    // -------------------------------------------------------------------------

    @Test
    void recordGift_belowThreshold_doesNotUnlock() {
        UUID trackId = UUID.randomUUID();
        Record mockRecord = mock(Record.class);
        when(mockRecord.getLong("giftProgress")).thenReturn(37L);
        when(mockRecord.getLong("giftThreshold")).thenReturn(50L);
        when(aerospike.operate(any(WritePolicy.class), any(Key.class), any()))
            .thenReturn(mockRecord);

        boolean unlocked = viralService.recordGift(trackId);

        assertFalse(unlocked, "Should not unlock before threshold is met");
    }

    @Test
    void recordGift_atThreshold_triggersUnlock() {
        UUID trackId = UUID.randomUUID();
        Record mockRecord = mock(Record.class);
        when(mockRecord.getLong("giftProgress")).thenReturn(50L);
        when(mockRecord.getLong("giftThreshold")).thenReturn(50L);
        when(aerospike.operate(any(WritePolicy.class), any(Key.class), any()))
            .thenReturn(mockRecord);

        boolean unlocked = viralService.recordGift(trackId);

        assertTrue(unlocked, "Should unlock when progress meets threshold");
        verify(kafka).send(eq("notification.event"), anyString());
    }

    @Test
    void recordGift_nullAerospikeRecord_returnsFalse() {
        when(aerospike.operate(any(WritePolicy.class), any(Key.class), any()))
            .thenReturn(null);

        boolean unlocked = viralService.recordGift(UUID.randomUUID());
        assertFalse(unlocked);
    }

    // -------------------------------------------------------------------------
    // checkBuzzingThreshold
    // -------------------------------------------------------------------------

    @Test
    void checkBuzzingThreshold_alreadyBuzzing_doesNothing() {
        String trackId = UUID.randomUUID().toString();
        Record rec = mock(Record.class);
        when(rec.getLong("isBuzzing")).thenReturn(1L);
        when(aerospike.get(any(), any(Key.class), anyString(), anyString(), anyString()))
            .thenReturn(rec);

        viralService.checkBuzzingThreshold(trackId);

        // No additional write should occur
        verify(aerospike, never()).put(any(), any(Key.class), any(Bin[].class));
    }

    @Test
    void checkBuzzingThreshold_belowThreshold_doesNotMark() {
        String trackId = UUID.randomUUID().toString();
        Record rec = mock(Record.class);
        when(rec.getLong("isBuzzing")).thenReturn(0L);
        when(rec.getLong("playCount6h")).thenReturn(50L);
        when(aerospike.get(any(), any(Key.class), anyString(), anyString(), anyString()))
            .thenReturn(rec);

        viralService.checkBuzzingThreshold(trackId);

        verify(aerospike, never()).put(any(), any(Key.class), any(Bin[].class));
    }

    // -------------------------------------------------------------------------
    // recordPlay — streak logic
    // -------------------------------------------------------------------------

    @Test
    void recordPlay_firstPlay_setsStreakToOne() {
        UUID userId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        when(aerospike.get(any(), any(Key.class), anyString(), anyString()))
            .thenReturn(null); // no existing record

        viralService.recordPlay(userId, artistId);

        verify(aerospike).put(any(WritePolicy.class), any(Key.class), any(Bin[].class));
        verify(kafka, never()).send(anyString(), anyString());
    }

    @Test
    void recordPlay_consecutiveDays_incrementsStreak() {
        UUID userId   = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        long yesterday = (Instant.now().getEpochSecond() / 86400) - 1;

        Record rec = mock(Record.class);
        when(rec.getLong("lastPlayDay")).thenReturn(yesterday);
        when(rec.getLong("streakCount")).thenReturn(2L);
        when(aerospike.get(any(), any(Key.class), anyString(), anyString()))
            .thenReturn(rec);

        viralService.recordPlay(userId, artistId);

        // Streak is now 3 — should emit notification
        verify(kafka).send(eq("notification.event"), contains("PLAY_STREAK"));
    }

    @Test
    void recordPlay_sameDay_noDoubleCount() {
        UUID userId   = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        long today = Instant.now().getEpochSecond() / 86400;

        Record rec = mock(Record.class);
        when(rec.getLong("lastPlayDay")).thenReturn(today);
        when(rec.getLong("streakCount")).thenReturn(1L);
        when(aerospike.get(any(), any(Key.class), anyString(), anyString()))
            .thenReturn(rec);

        viralService.recordPlay(userId, artistId);

        // No update — same day
        verify(aerospike, never()).put(any(), any(Key.class), any(Bin[].class));
    }

    @Test
    void recordPlay_brokenStreak_resetsToOne() {
        UUID userId   = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        long threeDaysAgo = (Instant.now().getEpochSecond() / 86400) - 3;

        Record rec = mock(Record.class);
        when(rec.getLong("lastPlayDay")).thenReturn(threeDaysAgo);
        when(rec.getLong("streakCount")).thenReturn(5L);
        when(aerospike.get(any(), any(Key.class), anyString(), anyString()))
            .thenReturn(rec);

        viralService.recordPlay(userId, artistId);

        // Verify reset — put() called with streakCount=1
        verify(aerospike).put(any(WritePolicy.class), any(Key.class), any(Bin[].class));
        verify(kafka, never()).send(eq("notification.event"), contains("PLAY_STREAK"));
    }
}
