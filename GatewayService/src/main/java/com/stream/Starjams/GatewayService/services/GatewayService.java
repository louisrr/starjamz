package com.stream.Starjams.GatewayService.services;

import com.stream.Starjams.GatewayService.config.WebClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GatewayService {

    private final WebClientConfig webClientConfig;
    private final WebClient webClient;

    @Autowired
    public GatewayService(WebClientConfig webClientConfig, WebClient webClient) {
        this.webClientConfig = webClientConfig;
        this.webClient = webClient;
    }

    public ResponseEntity<?> handleSignup(Object signupDetails) {
        // Communicate with authentication microservice
        return ResponseEntity.ok().build();
    }

    public Mono<String> getPlaylistById(String id) {
        return webClient.get()                          // Create a GET request
                .uri("/music/playlist/{id}", id)    // Set the URI and expand path variable
                .retrieve()                             // Retrieve the response
                .bodyToMono(String.class);              // Convert the response body to String
    }



    public ResponseEntity<?> fetchPlaylists() {
        // Suppose you fetch data from another microservice or a database
        List<Playlist> playlists = someRepository.findByUserId();
        if (playlists.isEmpty()) {
            return ResponseEntity.noContent().build(); // Return 204 if no content is found
        }
        return ResponseEntity.ok(playlists); // Return 200 with the playlists data
    }

    public ResponseEntity<?> createPlaylist(@RequestBody Object playlistDetails {
        webClientConfig.webClient(playlistDetails);
    }
}
