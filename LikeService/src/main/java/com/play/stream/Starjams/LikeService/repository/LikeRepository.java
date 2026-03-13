package com.play.stream.Starjams.LikeService.repository;

import com.play.stream.Starjams.LikeService.entity.Like;
import com.play.stream.Starjams.LikeService.model.ContentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByUserIdAndContentIdAndContentType(UUID userId, UUID contentId, ContentType contentType);

    void deleteByUserIdAndContentIdAndContentType(UUID userId, UUID contentId, ContentType contentType);

    List<Like> findByContentIdAndContentTypeOrderByCreatedAtDesc(UUID contentId, ContentType contentType, Pageable pageable);

    // Cursor-based pagination for user likes (before a given createdAt)
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.contentType = :contentType " +
           "AND l.createdAt < :cursor ORDER BY l.createdAt DESC")
    Slice<Like> findByUserIdAndContentTypeBeforeCursor(
            @Param("userId") UUID userId,
            @Param("contentType") ContentType contentType,
            @Param("cursor") Instant cursor,
            Pageable pageable);

    // First page (no cursor)
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.contentType = :contentType " +
           "ORDER BY l.createdAt DESC")
    Slice<Like> findByUserIdAndContentTypeFirstPage(
            @Param("userId") UUID userId,
            @Param("contentType") ContentType contentType,
            Pageable pageable);
}
