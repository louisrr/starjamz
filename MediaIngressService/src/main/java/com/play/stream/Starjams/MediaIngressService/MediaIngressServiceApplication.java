package com.play.stream.Starjams.MediaIngressService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MediaIngressService — live stream ingestion and real-time fan-out.
 *
 * <p>Handles:
 * <ul>
 *   <li>RTMP/RTSP ingest from mobile clients (iOS via avfvideosrc, Android via ahcsrc)</li>
 *   <li>External RTMP re-stream pull (YouTube, Twitch, generic RTMP)</li>
 *   <li>GStreamer transcode to HLS + RTSP + audio-only output profiles</li>
 *   <li>Follower fan-out via Kafka {@code livestream.event} → FeedFanoutConsumer</li>
 *   <li>Viewer session management and VOD archival to S3</li>
 *   <li>Admin endpoints for live stream control, health monitoring, and analytics</li>
 * </ul>
 *
 * <p>Registers with Eureka as {@code media-ingress-service}.
 * Binds to port 8080 internally; exposed as 8089 via docker-compose.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class MediaIngressServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaIngressServiceApplication.class, args);
    }
}
