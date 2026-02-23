package com.play.stream.Starjams.MusicService.services;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.play.stream.Starjams.MusicService.models.Stream;
import com.play.stream.Starjams.MusicService.repository.StreamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.kafka.common.utils.Sanitizer.sanitize;

@Service
public class MusicStreamingService {

    private final Path rootLocation;
    private final CqlSession session;

    @Autowired
    private StreamRepository streamRepository; // This is a hypothetical repository interfacing with ScyllaDB

    public MusicStreamingService(CqlSession session, String pathToFiles) {
        this.session = session;
        this.rootLocation = Paths.get(pathToFiles);
    }

    public String findStreamUrlByStreamId(String streamId) {
        Stream stream = streamRepository.findByStreamId(streamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stream not found"));
        return stream.getUrl();  // Assuming there's a getUrl method in the Stream entity
    }

    public Resource loadAudioAsResource(String filename) {
        String sanitize = sanitize(filename);

        // Query ScyllaDB for the audio file path
        String query = "SELECT file_path FROM audio_assets WHERE file_name = ?;";
        ResultSet resultSet = session.execute(query, sanitize);
        Row row = resultSet.one();
        if (row == null) {
            throw new RuntimeException("File not found: " + sanitize);
        }

        String filePath = row.getString("file_path");

        try {
            Path file = rootLocation.resolve(filePath);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}
