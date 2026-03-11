package com.play.stream.Starjams.NotificationService.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.NotificationService.model.NotificationEvent;
import com.play.stream.Starjams.NotificationService.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code notification.event} published by FeedService (and any other service)
 * and routes each event to {@link NotificationDeliveryService} for deduplication,
 * persistence, and push delivery.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationDeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(NotificationDeliveryService deliveryService,
                                     ObjectMapper objectMapper) {
        this.deliveryService = deliveryService;
        this.objectMapper    = objectMapper;
    }

    @KafkaListener(topics = "notification.event", groupId = "notification-service")
    public void onNotificationEvent(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            deliveryService.deliver(event);
        } catch (Exception e) {
            log.error("Failed to process notification.event: {}", e.getMessage(), e);
        }
    }
}
