package com.play.stream.Starjams.GatewayService.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class GatewayService {

    private final WebClient.Builder webClientBuilder;
    private final Map<String, String> serviceBaseUrls;

    public GatewayService(
            WebClient.Builder webClientBuilder,
            @Value("${admin.service.base-url:lb://admin-service}") String adminServiceBaseUrl,
            @Value("${feed.service.base-url:lb://feed-service}") String feedServiceBaseUrl,
            @Value("${like.service.base-url:lb://like-service}") String likeServiceBaseUrl,
            @Value("${music.service.base-url:lb://music-service}") String musicServiceBaseUrl,
            @Value("${payment.service.base-url:lb://payment-service}") String paymentServiceBaseUrl,
            @Value("${playlist.service.base-url:lb://playlist-service}") String playlistServiceBaseUrl,
            @Value("${user.service.base-url:lb://user-service}") String userServiceBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.serviceBaseUrls = new HashMap<>();
        this.serviceBaseUrls.put("admin", adminServiceBaseUrl);
        this.serviceBaseUrls.put("feed", feedServiceBaseUrl);
        this.serviceBaseUrls.put("like", likeServiceBaseUrl);
        this.serviceBaseUrls.put("music", musicServiceBaseUrl);
        this.serviceBaseUrls.put("payment", paymentServiceBaseUrl);
        this.serviceBaseUrls.put("playlist", playlistServiceBaseUrl);
        this.serviceBaseUrls.put("user", userServiceBaseUrl);
    }

    public ResponseEntity<String> proxyRequest(
            String service,
            HttpMethod httpMethod,
            String path,
            String query,
            HttpHeaders headers,
            String body) {
        String serviceBaseUrl = serviceBaseUrls.get(service.toLowerCase());
        if (serviceBaseUrl == null) {
            return ResponseEntity.badRequest().body("Unsupported service: " + service);
        }

        URI targetUri = UriComponentsBuilder
                .fromUriString(serviceBaseUrl)
                .path(path)
                .query(query)
                .build(true)
                .toUri();

        WebClient.RequestBodySpec requestSpec = webClientBuilder
                .build()
                .method(httpMethod)
                .uri(targetUri)
                .headers(httpHeaders -> {
                    httpHeaders.addAll(headers);
                    httpHeaders.remove(HttpHeaders.HOST);
                    httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                });

        WebClient.RequestHeadersSpec<?> requestHeadersSpec = body != null && !body.isBlank()
                ? requestSpec.contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                : requestSpec;

        try {
            return requestHeadersSpec
                    .retrieve()
                    .toEntity(String.class)
                    .block();
        } catch (WebClientResponseException exception) {
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .headers(exception.getHeaders())
                    .body(exception.getResponseBodyAsString());
        }
    }
}
