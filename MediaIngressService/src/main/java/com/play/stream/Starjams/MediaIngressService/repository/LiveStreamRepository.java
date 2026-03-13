package com.play.stream.Starjams.MediaIngressService.repository;

import com.play.stream.Starjams.MediaIngressService.model.LiveStream;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {

    Optional<LiveStream> findByStreamKey(String streamKey);

    Page<LiveStream> findByStatus(StreamStatus status, Pageable pageable);

    List<LiveStream> findByStatus(StreamStatus status);

    Page<LiveStream> findByStatusNot(StreamStatus status, Pageable pageable);

    @Query("SELECT l FROM LiveStream l WHERE l.startedAt >= :from ORDER BY l.peakViewerCount DESC")
    List<LiveStream> findTopStreamsByViewerCount(@Param("from") Instant from, Pageable pageable);

    @Query("SELECT COUNT(l) FROM LiveStream l WHERE l.startedAt >= :from")
    long countStreamsToday(@Param("from") Instant from);

    @Query("SELECT COALESCE(SUM(l.totalWatchMinutes), 0) FROM LiveStream l WHERE l.startedAt >= :from")
    java.math.BigDecimal sumTotalWatchMinutes(@Param("from") Instant from);

    @Query("SELECT MAX(l.peakViewerCount) FROM LiveStream l WHERE l.startedAt >= :from")
    Integer findPeakViewerCount(@Param("from") Instant from);
}
