package com.play.stream.Starjams.FeedService.repository;

import com.play.stream.Starjams.FeedService.entity.RemixCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RemixCardRepository extends JpaRepository<RemixCard, UUID> {

    List<RemixCard> findByOriginalTrackIdOrderByCreatedAtDesc(UUID originalTrackId);

    List<RemixCard> findByRemixerUserId(UUID remixerUserId);

    @Modifying
    @Query("UPDATE RemixCard r SET r.totalGiftsReceived = r.totalGiftsReceived + :delta "
         + "WHERE r.id = :remixId")
    void incrementGifts(@Param("remixId") UUID remixId, @Param("delta") int delta);
}
