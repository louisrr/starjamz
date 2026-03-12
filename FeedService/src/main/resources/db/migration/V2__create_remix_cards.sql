-- Remix cards: derived works created when a user saves a named stem mix.
-- Each remix is linked to the original track; the original artist earns a
-- configurable split (default 30%) on all gifts the remix card receives.

CREATE TABLE IF NOT EXISTS remix_cards (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    original_track_id   UUID        NOT NULL,
    remixer_user_id     UUID        NOT NULL,
    remix_title         TEXT,
    stem_volumes_json   JSONB       NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    total_gifts_received INT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rc_original_track   ON remix_cards (original_track_id);
CREATE INDEX IF NOT EXISTS idx_rc_remixer          ON remix_cards (remixer_user_id);
CREATE INDEX IF NOT EXISTS idx_rc_created_at       ON remix_cards (created_at DESC);
