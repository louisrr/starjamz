package com.play.stream.Starjams.FeedService.Models;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;


@Table
public class FeedItemModel {

    @PrimaryKey
    private UUID id;

    @Column
    private String title;

    @Column
    private String description;
}

