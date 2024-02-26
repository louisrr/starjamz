package com.play.stream.Starjams.FeedService.Services;

import com.play.stream.Starjams.FeedService.Models.FeedItemModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraTemplate;

public class FeedItemService {

    public FeedItemService() {
    }

    @Autowired
    private CassandraTemplate cassandraTemplate;

    public FeedItemModel createSettings(FeedItemModel settings) {
        return cassandraTemplate.insert(settings);
    }
}
