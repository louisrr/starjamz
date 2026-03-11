package com.play.stream.Starjams.NotificationService.controller;

import com.play.stream.Starjams.NotificationService.entity.UserNotification;
import com.play.stream.Starjams.NotificationService.repository.UserNotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * In-app notification inbox REST API.
 *
 * <pre>
 *   GET    /api/v1/users/{userId}/notifications?page=0&size=20
 *   GET    /api/v1/users/{userId}/notifications/unread-count
 *   POST   /api/v1/users/{userId}/notifications/mark-all-read
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Notifications", description = "In-app notification inbox")
public class NotificationController {

    private final UserNotificationRepository notifRepo;

    public NotificationController(UserNotificationRepository notifRepo) {
        this.notifRepo = notifRepo;
    }

    @Operation(summary = "Get paginated notification inbox for a user")
    @GetMapping("/{userId}/notifications")
    public ResponseEntity<Page<UserNotification>> getNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<UserNotification> result = notifRepo
            .findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/{userId}/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID userId) {
        long count = notifRepo.countByRecipientIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("userId", (long) userId.hashCode(), "unreadCount", count));
    }

    @Operation(summary = "Mark all notifications as read for a user")
    @PostMapping("/{userId}/notifications/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(@PathVariable UUID userId) {
        int updated = notifRepo.markAllReadForUser(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "markedRead", updated));
    }
}
