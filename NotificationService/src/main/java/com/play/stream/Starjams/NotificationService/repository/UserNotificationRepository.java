package com.play.stream.Starjams.NotificationService.repository;

import com.play.stream.Starjams.NotificationService.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    Page<UserNotification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    @Modifying
    @Transactional
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllReadForUser(UUID recipientId);
}
