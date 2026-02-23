package com.play.stream.Starjams.GatewayService.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GatewayService {

    private final WebClient webClient;

    public GatewayService(
            WebClient.Builder webClientBuilder,
            @Value("${playlist.service.base-url:lb://playlist-service}") String playlistServiceBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(playlistServiceBaseUrl).build();
    }

    public String getPlaylistById(String playlistId) {
        return webClient
                .get()
                .uri("/playlists/{playlistId}", playlistId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String getAllPlaylists() {
        return webClient
                .get()
                .uri("/playlists")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String createPlaylist(Map<String, Object> payload) {
        return webClient
                .post()
                .uri("/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
