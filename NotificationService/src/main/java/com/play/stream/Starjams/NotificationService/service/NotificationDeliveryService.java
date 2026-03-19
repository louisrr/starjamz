package com.play.stream.Starjams.NotificationService.service;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.play.stream.Starjams.NotificationService.entity.UserNotification;
import com.play.stream.Starjams.NotificationService.model.NotificationEvent;
import com.play.stream.Starjams.NotificationService.repository.UserNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Core notification delivery logic:
 *
 * <ol>
 *   <li>Deduplication check in Aerospike (suppress duplicates within {@code dedupWindowSeconds}).
 *   <li>Persist to PostgreSQL (UserNotification) for in-app inbox.
 *   <li>Check user's notification preferences (opt-in per type) — stored in Aerospike.
 *   <li>Dispatch push notification via FCM (Firebase Cloud Messaging).
 * </ol>
 */
@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);
    private static final String NS = "fetio";

    @Value("${notification.dedup-window-seconds:3600}")
    private int dedupWindowSeconds;

    private final IAerospikeClient aerospike;
    private final UserNotificationRepository notifRepo;

    @Nullable
    private final FirebaseMessaging firebaseMessaging;

    public NotificationDeliveryService(IAerospikeClient aerospike,
                                       UserNotificationRepository notifRepo,
                                       @Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.aerospike         = aerospike;
        this.notifRepo         = notifRepo;
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Processes an incoming notification event end-to-end.
     */
    public void deliver(NotificationEvent event) {
        // 1. Deduplication
        if (event.getDeduplicationKey() != null && isDuplicate(event)) {
            log.debug("Suppressing duplicate notification: {}", event.getDeduplicationKey());
            return;
        }

        // 2. Check user preference for this notification type
        if (!userAllowsNotificationType(event)) {
            log.debug("User {} has opted out of {} notifications",
                event.getRecipientId(), event.getType());
            return;
        }

        // 3. Persist to PostgreSQL inbox
        UserNotification record = toEntity(event);
        notifRepo.save(record);

        // 4. Record dedup key in Aerospike with TTL
        if (event.getDeduplicationKey() != null) {
            recordDedupKey(event);
        }

        // 5. Send push notification via FCM
        sendPushNotification(event, record);
    }

    // -------------------------------------------------------------------------
    // FCM push dispatch
    // -------------------------------------------------------------------------

    private void sendPushNotification(NotificationEvent event, UserNotification record) {
        if (firebaseMessaging == null) {
            log.debug("FCM not configured — skipping push for recipient {}", event.getRecipientId());
            return;
        }

        String deviceToken = lookupDeviceToken(event.getRecipientId());
        if (deviceToken == null || deviceToken.isBlank()) {
            log.debug("No FCM device token for recipient {} — skipping push", event.getRecipientId());
            return;
        }

        String title = event.getTitle() != null ? event.getTitle() : event.getType().name();
        String body  = event.getBody() != null ? event.getBody() : "";
        Map<String, String> data = event.getData() != null ? event.getData() : Collections.emptyMap();

        Message message = Message.builder()
            .setToken(deviceToken)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .putAllData(data)
            .build();

        try {
            String messageId = firebaseMessaging.send(message);
            record.setPushed(true);
            notifRepo.save(record);
            log.info("FCM push sent to {} — messageId={} type={}", event.getRecipientId(), messageId, event.getType());
        } catch (FirebaseMessagingException e) {
            log.error("FCM push failed for recipient {} type={}: {}", event.getRecipientId(), event.getType(), e.getMessage());
        }
    }

    private String lookupDeviceToken(java.util.UUID userId) {
        // Device tokens are stored in Aerospike under notif_prefs:{userId} in the fcmToken bin.
        // Clients register tokens via the /api/v1/users/{userId}/notifications/device-token endpoint.
        Key key = new Key(NS, "notif_prefs:" + userId, userId.toString());
        Record rec = aerospike.get(null, key, "fcmToken");
        if (rec == null) return null;
        return rec.getString("fcmToken");
    }

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    private boolean isDuplicate(NotificationEvent event) {
        Key key = new Key(NS, "notif_dedup", event.getDeduplicationKey());
        Record rec = aerospike.get(null, key);
        return rec != null;
    }

    private void recordDedupKey(NotificationEvent event) {
        Key key = new Key(NS, "notif_dedup", event.getDeduplicationKey());
        WritePolicy wp = new WritePolicy();
        wp.expiration = dedupWindowSeconds;
        aerospike.put(wp, key, new Bin("sent", 1L));
    }

    // -------------------------------------------------------------------------
    // User preferences
    // -------------------------------------------------------------------------

    private boolean userAllowsNotificationType(NotificationEvent event) {
        Key key = new Key(NS, "notif_prefs:" + event.getRecipientId(),
            event.getRecipientId().toString());
        Record rec = aerospike.get(null, key, event.getType().name());
        if (rec == null) return true; // default: all enabled
        Object val = rec.getValue(event.getType().name());
        return val == null || ((Number) val).intValue() != 0;
    }

    // -------------------------------------------------------------------------
    // Entity mapping
    // -------------------------------------------------------------------------

    private UserNotification toEntity(NotificationEvent ev) {
        UserNotification n = new UserNotification();
        n.setRecipientId(ev.getRecipientId());
        n.setActorId(ev.getActorId());
        n.setActorDisplayName(ev.getActorDisplayName());
        n.setActorAvatarUrl(ev.getActorAvatarUrl());
        n.setType(ev.getType());
        n.setTitle(ev.getTitle() != null ? ev.getTitle() : ev.getType().name());
        n.setBody(ev.getBody());
        n.setCreatedAt(ev.getOccurredAt() != null ? ev.getOccurredAt() : Instant.now());
        return n;
    }
}
