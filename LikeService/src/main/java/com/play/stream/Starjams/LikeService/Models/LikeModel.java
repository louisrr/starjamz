package com.play.stream.Starjams.LikeService.Models;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

// Scylla is NoSQL, we don't need JPA, or Hibernate here. Google the CAP Theorem 👍

@Table
public class LikeModel {

    @PrimaryKey
    private UUID id;

    @Column
    private UUID itemId;

    @Column
    private String itemType;

    @Column
    private String itemDescription;

    @Column
    private String assetUrl;

    @Column("timestamp_column")
    private Instant timestamp;

}
