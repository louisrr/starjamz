package com.play.stream.Starjams.LikeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.util.List;
import java.util.UUID;

public class LikeServiceImpl implements LikeService {

    public LikeServiceImpl() {
    }

    @Autowired
    private CassandraTemplate cassandraTemplate;


    @Override
    public void addLike(UUID userId, UUID streamId) {
        
    }

    @Override
    public void removeLike(UUID userId, UUID streamId) {

    }

    @Override
    public long countLikes(UUID streamId) {
        return 0;
    }

    @Override
    public boolean checkLikeStatus(UUID userId, UUID streamId) {
        return false;
    }

    @Override
    public List<String> getLikedStreams(UUID userId) {
        return null;
    }

    @Override
    public List<String> getLikesByStream(UUID streamId) {
        return null;
    }
}
