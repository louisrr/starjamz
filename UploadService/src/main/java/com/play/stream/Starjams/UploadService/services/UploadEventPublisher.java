package com.play.stream.Starjams.UploadService.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.play.stream.Starjams.UploadService.models.UploadRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UploadEventPublisher {

    private static final String AUDIO_TOPIC = "audio-uploads";
    private static final String VIDEO_TOPIC = "video-uploads";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UploadEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAudioUpload(UploadRecord record) {
        publish(record, AUDIO_TOPIC);
    }

    public void publishVideoUpload(UploadRecord record) {
        publish(record, VIDEO_TOPIC);
    }

    private void publish(UploadRecord record, String topic) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("uploadId", record.getId().toString());
            event.put("s3Url", record.getS3Url());
            event.put("fileName", record.getOriginalFileName());
            event.put("contentType", record.getContentType());
            event.put("fileSize", record.getFileSize());
            event.put("uploadedBy", record.getUploadedBy());
            event.put("uploadedAt", record.getUploadedAt().toString());
            kafkaTemplate.send(topic, record.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            // Log and continue — Kafka publish failure should not roll back a completed upload
            System.err.println("Failed to publish upload event to topic " + topic + ": " + e.getMessage());
        }
    }
}
