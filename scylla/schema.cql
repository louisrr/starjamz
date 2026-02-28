-- ============================================================
-- Starjamz – UploadService keyspace
-- ScyllaDB / Cassandra-compatible CQL
-- ============================================================
-- Three tables, each shaped for a concrete query pattern:
--
--   1. upload_records              → look up one record by id
--   2. upload_records_by_user      → a user's full upload history, newest first
--   3. upload_records_by_user_type → a user's history filtered by AUDIO | VIDEO
--
-- SimpleStrategy / RF=1 is fine for local Docker.
-- In production use NetworkTopologyStrategy with RF ≥ 3.
-- ============================================================

CREATE KEYSPACE IF NOT EXISTS starjamz_uploads
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
    AND durable_writes = true;

USE starjamz_uploads;

-- ------------------------------------------------------------
-- 1. Primary lookup table: find any record by its UUID
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS upload_records (
    id                 uuid        PRIMARY KEY,
    original_file_name text,
    content_type       text,
    file_size          bigint,
    s3_bucket          text,
    s3_key             text,
    s3_url             text,
    media_type         text,   -- 'AUDIO' or 'VIDEO'
    uploaded_by        text,
    uploaded_at        timestamp
);

-- ------------------------------------------------------------
-- 2. User upload history: all uploads for a user, newest first
--    Query: WHERE uploaded_by = ?
--    Optional range: AND uploaded_at < ? (pagination)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS upload_records_by_user (
    uploaded_by        text,
    uploaded_at        timestamp,
    id                 uuid,
    original_file_name text,
    content_type       text,
    file_size          bigint,
    s3_bucket          text,
    s3_key             text,
    s3_url             text,
    media_type         text,
    PRIMARY KEY ((uploaded_by), uploaded_at, id)
) WITH CLUSTERING ORDER BY (uploaded_at DESC, id ASC)
  AND comment = 'All uploads for a given user, newest first';

-- ------------------------------------------------------------
-- 3. User + media-type history: filter by AUDIO or VIDEO
--    Query: WHERE uploaded_by = ? AND media_type = ?
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS upload_records_by_user_type (
    uploaded_by        text,
    media_type         text,
    uploaded_at        timestamp,
    id                 uuid,
    original_file_name text,
    content_type       text,
    file_size          bigint,
    s3_bucket          text,
    s3_key             text,
    s3_url             text,
    PRIMARY KEY ((uploaded_by, media_type), uploaded_at, id)
) WITH CLUSTERING ORDER BY (uploaded_at DESC, id ASC)
  AND comment = 'Uploads for a user filtered to a single media type, newest first';
