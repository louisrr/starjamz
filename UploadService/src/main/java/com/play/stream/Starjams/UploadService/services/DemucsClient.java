package com.play.stream.Starjams.UploadService.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.UploadService.dto.StemSeparationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client that calls the Demucs Python sidecar service.
 * The sidecar accepts an S3 key, runs stem separation, uploads stems to S3,
 * and returns a map of stemType → s3Key.
 */
@Service
public class DemucsClient {

    private static final Logger log = LoggerFactory.getLogger(DemucsClient.class);

    @Value("${demucs.sidecar.url:http://localhost:8000}")
    private String sidecarUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Request stem separation for an uploaded track.
     *
     * @param s3Key    S3 key of the source audio file
     * @param trackId  UUID of the track (used for S3 output path)
     * @param uploadId UUID of the upload (used for S3 output path)
     * @param tier     1 = vocal+instrumental only, 2 = full 4-stem
     * @return map of stemType → s3Key, or null if the sidecar is unavailable
     */
    public StemSeparationResponse requestSeparation(String s3Key, String trackId,
                                                     String uploadId, int tier) {
        String body;
        try {
            body = objectMapper.writeValueAsString(new SeparationRequest(s3Key, trackId, uploadId, tier));
        } catch (Exception e) {
            log.error("Failed to serialize Demucs request", e);
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sidecarUrl + "/separate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMinutes(10))   // separation can take a while
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Demucs sidecar returned HTTP {}: {}", response.statusCode(), response.body());
                return null;
            }
            return objectMapper.readValue(response.body(), StemSeparationResponse.class);
        } catch (Exception e) {
            log.error("Demucs sidecar call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private record SeparationRequest(String s3Key, String trackId, String uploadId, int tier) {}
}
