package com.play.stream.Starjams.FeedService.repository;

import com.play.stream.Starjams.FeedService.entity.FeedEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedEventLogRepository extends JpaRepository<FeedEventLog, UUID> {
}
