package com.play.stream.Starjams.MusicService.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.play.stream.Starjams.MusicService.services.MusicStreamingService;

@RestController
public class MusicStreamController {

    private final MusicStreamingService musicStreamingService;

    @Autowired
    public MusicStreamController(MusicStreamingService musicStreamingService) {
        this.musicStreamingService = musicStreamingService;
    }

    @GetMapping("/stream/audio/{streamId}")
    public ResponseEntity<Resource> streamAudio(@PathVariable String filename) {
        try {
            Resource file = musicStreamingService.loadAudioAsResource(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(file);
        } catch (RuntimeException e) {
            // This is a very basic exception handling. Consider more specific exceptions for better client feedback.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e);
        }
    }
}
