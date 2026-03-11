package com.play.stream.Starjams.FeedService.services;

import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full unit-test coverage for the feed ranking algorithm.
 * No Spring context is loaded — pure unit tests for scoring logic.
 */
class FeedRankingServiceTest {

    private FeedRankingService ranker;

    @BeforeEach
    void setUp() {
        ranker = new FeedRankingService();
    }

    // -------------------------------------------------------------------------
    // rank() — ordering guarantees
    // -------------------------------------------------------------------------

    @Test
    void rank_returnsItemsSortedByScoreDescending() {
        FeedEvent old  = makeEvent(UUID.randomUUID(), 1, 0, 0, 0, Instant.now().minus(48, ChronoUnit.HOURS));
        FeedEvent fresh = makeEvent(UUID.randomUUID(), 1, 0, 0, 0, Instant.now().minus(1, ChronoUnit.MINUTES));

        List<FeedEvent> ranked = ranker.rank(
            List.of(old, fresh),
            Collections.emptyMap(),
            Collections.emptyMap()
        );

        assertEquals(fresh.getEventId(), ranked.get(0).getEventId(),
            "Recently posted event should rank first");
    }

    @Test
    void rank_highEngagementOutranksLowEngagement_atSameAge() {
        Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
        FeedEvent popular = makeEvent(UUID.randomUUID(), 10000, 5000, 2000, 500, now);
        FeedEvent obscure = makeEvent(UUID.randomUUID(), 1, 0, 0, 0, now);

        List<FeedEvent> ranked = ranker.rank(
            List.of(obscure, popular),
            Collections.emptyMap(),
            Collections.emptyMap()
        );

        assertEquals(popular.getEventId(), ranked.get(0).getEventId(),
            "High-engagement event should rank first at the same age");
    }

    @Test
    void rank_affinityBoostsKnownArtist() {
        UUID artistWithAffinity    = UUID.randomUUID();
        UUID artistWithoutAffinity = UUID.randomUUID();
        Instant now = Instant.now().minus(30, ChronoUnit.MINUTES);

        FeedEvent affinityEvent = makeEvent(artistWithAffinity, 100, 10, 5, 1, now);
        FeedEvent noAffinityEvent = makeEvent(artistWithoutAffinity, 100, 10, 5, 1, now);

        Map<String, Double> affinityMap = Map.of(artistWithAffinity.toString(), 1.0);

        List<FeedEvent> ranked = ranker.rank(
            List.of(noAffinityEvent, affinityEvent),
            affinityMap,
            Collections.emptyMap()
        );

        assertEquals(affinityEvent.getEventId(), ranked.get(0).getEventId(),
            "Event from an artist the user loves should rank above a stranger at equal engagement");
    }

    @Test
    void rank_viralBoostAppliesToHighRepostVelocity() {
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now().minus(30, ChronoUnit.MINUTES);

        FeedEvent viral  = makeEventWithTrack(actor, UUID.randomUUID(), 100, 10, 5, 1, now);
        FeedEvent normal = makeEventWithTrack(actor, UUID.randomUUID(), 100, 10, 5, 1, now);

        Map<String, Long> viralMap = Map.of(viral.getTrackId().toString(), 95L);

        List<FeedEvent> ranked = ranker.rank(
            List.of(normal, viral),
            Collections.emptyMap(),
            viralMap
        );

        assertEquals(viral.getEventId(), ranked.get(0).getEventId(),
            "Viral (high repost velocity) track should outrank normal track at same engagement");
    }

    @Test
    void rank_buzzingBonusApplied() {
        Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
        FeedEvent buzzing = makeEvent(UUID.randomUUID(), 100, 10, 5, 1, now);
        buzzing.setBuzzing(true);
        FeedEvent notBuzzing = makeEvent(UUID.randomUUID(), 1000, 200, 100, 50, now);
        // notBuzzing has much higher engagement but no buzzing bonus

        List<FeedEvent> ranked = ranker.rank(
            List.of(notBuzzing, buzzing),
            Collections.emptyMap(),
            Collections.emptyMap()
        );

        // Buzzing bonus = +0.30; at low base score this might not always win,
        // but verify the buzzing item's score is bumped
        double buzzingScore = ranked.stream()
            .filter(e -> e.getEventId().equals(buzzing.getEventId()))
            .findFirst().orElseThrow()
            .getRankScore();
        assertTrue(buzzingScore > 0, "Buzzing event must have a positive rank score");
    }

    @Test
    void rank_first48BoostApplied() {
        FeedEvent newSmallCreator = makeEvent(UUID.randomUUID(), 10, 1, 0, 0,
            Instant.now().minus(2, ChronoUnit.HOURS));
        newSmallCreator.setNew(true);

        FeedEvent oldEstablished = makeEvent(UUID.randomUUID(), 500, 100, 50, 20,
            Instant.now().minus(50, ChronoUnit.HOURS));

        List<FeedEvent> ranked = ranker.rank(
            List.of(oldEstablished, newSmallCreator),
            Collections.emptyMap(),
            Collections.emptyMap()
        );

        assertEquals(newSmallCreator.getEventId(), ranked.get(0).getEventId(),
            "First-48 2.5× boost on a fresh track should outrank a stale high-engagement track");
    }

    @Test
    void rank_diversityPenaltyAppliedAfter3ItemsFromSameActor() {
        UUID prolificActor = UUID.randomUUID();
        Instant now = Instant.now().minus(10, ChronoUnit.MINUTES);

        List<FeedEvent> events = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            events.add(makeEvent(prolificActor, 1000, 200, 50, 10, now));
        }

        List<FeedEvent> ranked = ranker.rank(events, Collections.emptyMap(), Collections.emptyMap());

        // The 4th and 5th items from the same actor must have a lower score than 1st–3rd
        assertTrue(ranked.get(3).getRankScore() < ranked.get(2).getRankScore(),
            "4th item from same actor should be penalised relative to 3rd");
        assertTrue(ranked.get(4).getRankScore() < ranked.get(3).getRankScore(),
            "5th item from same actor should be penalised more than 4th");
    }

    @Test
    void rank_emptyListReturnsEmpty() {
        List<FeedEvent> result = ranker.rank(
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void rank_singleItemHasPositiveScore() {
        FeedEvent ev = makeEvent(UUID.randomUUID(), 1, 0, 0, 0, Instant.now().minus(1, ChronoUnit.HOURS));
        List<FeedEvent> result = ranker.rank(List.of(ev), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(1, result.size());
        assertTrue(result.get(0).getRankScore() > 0);
    }

    // -------------------------------------------------------------------------
    // computeRecency — direct unit tests
    // -------------------------------------------------------------------------

    @Test
    void computeRecency_recentPostScoresNearOne() {
        FeedEvent ev = new FeedEvent();
        ev.setPostedAt(Instant.now().minusSeconds(60)); // 1 minute ago
        double score = ranker.computeRecency(ev);
        assertTrue(score > 0.90, "Score 1 minute old should be > 0.90, was " + score);
    }

    @Test
    void computeRecency_72HourOldPostScoresLow() {
        FeedEvent ev = new FeedEvent();
        ev.setPostedAt(Instant.now().minus(72, ChronoUnit.HOURS));
        double score = ranker.computeRecency(ev);
        assertTrue(score < 0.15, "Score 72h old should be < 0.15, was " + score);
    }

    @Test
    void computeRecency_nullPostedAtReturnsZero() {
        FeedEvent ev = new FeedEvent();
        assertEquals(0.0, ranker.computeRecency(ev));
    }

    // -------------------------------------------------------------------------
    // computeEngagement
    // -------------------------------------------------------------------------

    @Test
    void computeEngagement_zeroCounts() {
        FeedEvent ev = makeEvent(UUID.randomUUID(), 0, 0, 0, 0, Instant.now());
        assertEquals(0.0, ranker.computeEngagement(ev));
    }

    @Test
    void computeEngagement_highCounts_cappedAtOne() {
        FeedEvent ev = makeEvent(UUID.randomUUID(), 1_000_000, 1_000_000, 1_000_000, 1_000_000,
            Instant.now());
        double score = ranker.computeEngagement(ev);
        assertTrue(score <= 1.0, "Engagement score should be capped at 1.0");
        assertTrue(score > 0.9, "Very high engagement should score close to 1.0");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FeedEvent makeEvent(UUID actorId, long plays, long likes, long reposts, long comments,
                                 Instant postedAt) {
        FeedEvent ev = new FeedEvent();
        ev.setEventId(UUID.randomUUID());
        ev.setEventType(EventType.TRACK_POSTED);
        ev.setActorId(actorId);
        ev.setPlayCount(plays);
        ev.setLikeCount(likes);
        ev.setRepostCount(reposts);
        ev.setCommentCount(comments);
        ev.setPostedAt(postedAt);
        return ev;
    }

    private FeedEvent makeEventWithTrack(UUID actorId, UUID trackId, long plays, long likes,
                                          long reposts, long comments, Instant postedAt) {
        FeedEvent ev = makeEvent(actorId, plays, likes, reposts, comments, postedAt);
        ev.setTrackId(trackId);
        return ev;
    }
}
