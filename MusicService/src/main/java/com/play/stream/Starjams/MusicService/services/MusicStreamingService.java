package com.play.stream.Starjams.MusicService.services;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.kafka.common.utils.Sanitizer.sanitize;

@Service
public class MusicStreamingService {

    private final Path rootLocation;
    private final CqlSession session;

    public MusicStreamingService(CqlSession session, String pathToFiles) {
        this.session = session;
        this.rootLocation = Paths.get(pathToFiles);
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
