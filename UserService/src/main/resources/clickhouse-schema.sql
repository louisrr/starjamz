-- ClickHouse schema for UserService system of record
-- Run against your ClickHouse instance before starting the service.

CREATE DATABASE IF NOT EXISTS starjamz;

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
