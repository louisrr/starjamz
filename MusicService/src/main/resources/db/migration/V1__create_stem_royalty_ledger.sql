-- Stem royalty ledger: micro-royalties earned by session hosts per listener-minute.
-- Populated hourly by StemSessionService#flushRoyaltiesToPostgres().

CREATE TABLE IF NOT EXISTS stem_royalty_ledger (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id       UUID        NOT NULL,
    host_user_id     UUID        NOT NULL,
    listener_user_id UUID        NOT NULL,
    listener_minutes NUMERIC(10, 4) NOT NULL,
    royalty_amount   NUMERIC(12, 6) NOT NULL,
    settled_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_srl_host_user     ON stem_royalty_ledger (host_user_id);
CREATE INDEX IF NOT EXISTS idx_srl_session       ON stem_royalty_ledger (session_id);
CREATE INDEX IF NOT EXISTS idx_srl_settled_at    ON stem_royalty_ledger (settled_at DESC);
