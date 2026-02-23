package com.stream.Starjams.GatewayService.client;

import com.stream.Starjams.GatewayService.datatransferobjects.ItemDTO;
import com.stream.Starjams.GatewayService.datatransferobjects.PlaylistDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MusicServiceClient {

    private final WebClient webClient;

    @Autowired
    public MusicServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches playlists for a specific user ID from the MusicService.
     * @param userId The ID of the user whose playlists are to be fetched.
     * @return A Flux of PlaylistDTO objects.
     */
    public Flux<PlaylistDTO> getPlaylistsByUserId(String userId) {
        return webClient.get()
                .uri("/music/playlist/{userId}", userId)
                .retrieve()
                .onStatus(
                        httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                        clientResponse -> Mono.error(new RuntimeException("API call failed with response status: " + clientResponse.statusCode()))
                )
                .bodyToFlux(PlaylistDTO.class);
    }

    /**
     * Retrieves a single playlist by its ID.
     * @param playlistId The ID of the playlist to retrieve.
     * @return A Mono of PlaylistDTO.
     */
    public Mono<PlaylistDTO> getPlaylistById(String playlistId) {
        return webClient.get()
                .uri("/music/playlist/detail/{playlistId}", playlistId)
                .retrieve()
                .bodyToMono(PlaylistDTO.class);
    }

    public Mono<ResponseEntity<PlaylistDTO>> addItemToPlaylist(Long playlistId, ItemDTO item) {
        return webClient.post()
                .uri("/playlists/{playlistId}/items", playlistId)
                .bodyValue(item)
                .retrieve()
                .toEntity(PlaylistDTO.class); // Assumes MusicService returns the updated PlaylistDTO
    }

    /**
     * Deletes a track from a playlist.
     * @param playlistId The ID of the playlist from which the track should be removed.
     * @param trackId The ID of the track to be removed.
     * @return A Mono of ResponseEntity indicating the outcome of the operation.
     */
    public Mono<ResponseEntity<Void>> deleteTrackFromPlaylist(String playlistId, String trackId) {
        return webClient.delete()
                .uri("/playlists/{playlistId}/items/{trackId}", playlistId, trackId)
                .retrieve()
                .toBodilessEntity();  // This converts the response to a ResponseEntity without a body.
    }
}
