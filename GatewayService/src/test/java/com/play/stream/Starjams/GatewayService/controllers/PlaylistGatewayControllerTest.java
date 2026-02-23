package com.play.stream.Starjams.GatewayService.controllers;

import com.play.stream.Starjams.GatewayService.services.GatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PlaylistGatewayController.class)
class PlaylistGatewayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GatewayService gatewayService;

    @Test
    void shouldReturnAllPlaylists() {
        when(gatewayService.getAllPlaylists()).thenReturn("[{\"id\":\"pl-1\"}]");

        webTestClient.get()
                .uri("/gateway/playlists")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("[{\"id\":\"pl-1\"}]");
    }

    @Test
    void shouldReturnPlaylistById() {
        when(gatewayService.getPlaylistById(eq("pl-9"))).thenReturn("{\"id\":\"pl-9\"}");

        webTestClient.get()
                .uri("/gateway/playlists/pl-9")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"id\":\"pl-9\"}");
    }

    @Test
    void shouldCreatePlaylist() {
        when(gatewayService.createPlaylist(anyMap())).thenReturn("{\"id\":\"new\"}");

        webTestClient.post()
                .uri("/gateway/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Workout"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).isEqualTo("{\"id\":\"new\"}");
    }
}
