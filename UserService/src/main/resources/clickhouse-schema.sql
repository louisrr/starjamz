-- ClickHouse schema for UserService system of record
-- Run against your ClickHouse instance before starting the service.

CREATE DATABASE IF NOT EXISTS starjamz;

-- ============================================================
-- users — primary user table (system of record)
--
-- id:               UUID assigned at signup (no native auto-increment
--                   in ClickHouse; UUID gives globally unique, sortable IDs)
-- signup_timestamp: fromUnixTimestamp(epoch) stored as DateTime
-- last_online:      same pattern as signup_timestamp
-- gender:           LowCardinality enum — 'M' | 'F' | '' (empty = not set)
-- ip_address:       IPv6-safe string (covers both IPv4 & IPv6 without wasting space)
-- ReplacingMergeTree deduplicates on id so upserts work correctly.
-- ============================================================
CREATE TABLE IF NOT EXISTS starjamz.users
(
    id                UUID,
    user_name         Nullable(String),
    email             Nullable(String),
    email_hash        Nullable(String),
    phone_number      Nullable(String),
    password_hash     String,
    signup_timestamp  DateTime          DEFAULT now(),
    last_online       DateTime          DEFAULT now(),
    following         UInt32            DEFAULT 0,
    likes             UInt32            DEFAULT 0,
    shares            UInt32            DEFAULT 0,
    ip_address        Nullable(String),
    hostname          Nullable(String),
    geolocation       Nullable(String),
    user_agent        Nullable(String),
    device            Nullable(String),
    most_viewed_genre UInt32            DEFAULT 0,
    follower_count    UInt32            DEFAULT 0,
    gender            LowCardinality(String) DEFAULT ''
)
ENGINE = ReplacingMergeTree()
ORDER BY id;

-- ============================================================
-- user_events — immutable audit / analytics trail
-- ============================================================
CREATE TABLE IF NOT EXISTS starjamz.user_events
(
    event_id      UUID,
    event_type    LowCardinality(String),  -- CREATED | UPDATED | DELETED
    occurred_at   DateTime DEFAULT now(),
    user_id       UUID,
    screen_name   Nullable(String),
    user_name     Nullable(String),
    email         Nullable(String),
    email_hash    Nullable(String),
    avi_address   Nullable(String),
    cover_address Nullable(String),
    bio           Nullable(String),
    url           Nullable(String)
)
ENGINE = MergeTree()
ORDER BY (occurred_at, user_id);
