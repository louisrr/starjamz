package com.play.stream.Starjams.MusicService.repository;

import com.play.stream.Starjams.MusicService.models.Stream;
import org.springframework.data.cassandra.repository.CassandraRepository;
import java.util.Optional;

public interface StreamRepository extends CassandraRepository<Stream, String> {
    Optional<Stream> findByStreamId(String streamId);
}