-- ============================================================
-- Migration V2 — add password-reset columns to starjamz.users
--
-- Run this against your ClickHouse instance AFTER the initial
-- schema (clickhouse-schema.sql) has been applied.
--
-- New columns:
--   auth_code        — 6-digit OTP generated during forgot-password flow
--   auth_code_date   — epoch milliseconds when the OTP was issued
--   password_changed — flag set to 1 after a successful password reset
--
-- ClickHouse ALTER TABLE ADD COLUMN is an instant metadata operation
-- (no data rewrite). Existing rows receive the DEFAULT value.
-- ============================================================

ALTER TABLE starjamz.users
    ADD COLUMN IF NOT EXISTS auth_code        UInt32  DEFAULT 0,
    ADD COLUMN IF NOT EXISTS auth_code_date   Int64   DEFAULT 0,
    ADD COLUMN IF NOT EXISTS password_changed UInt8   DEFAULT 0;

-- Verify
DESCRIBE TABLE starjamz.users;
