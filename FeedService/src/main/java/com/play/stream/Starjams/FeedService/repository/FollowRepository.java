package com.play.stream.Starjams.FeedService.repository;

import com.play.stream.Starjams.FeedService.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    Page<Follow> findByFollowerId(UUID followerId, Pageable pageable);

    Page<Follow> findByFolloweeId(UUID followeeId, Pageable pageable);

    long countByFollowerId(UUID followerId);

    long countByFolloweeId(UUID followeeId);

    void deleteByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}
