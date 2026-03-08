-- Unique constraint on (account_id, year_month) for INTEREST transactions
-- This is the database-level idempotency guard for interest application
-- Note: Derby does not support partial unique indexes directly,
-- so we enforce this constraint at the application service layer
-- and use a separate unique index approach.

-- We add a unique constraint on (account_id, year_month) for non-null year_month values.
-- Since Derby doesn't support filtered unique indexes, we enforce this primarily
-- at the service layer (Layer 1) with this as a best-effort backstop (Layer 2).
-- The application-level check on lastInterestAppliedMonth is the primary guard.

-- No DDL needed here; constraint is enforced in application code via
-- the unique check in InterestSchedulerService before inserting.
-- This migration is a placeholder documenting the design decision.

-- Optional: add a non-unique index for query performance
CREATE INDEX idx_tx_account_yearmonth ON transactions (account_id, year_month);
CREATE INDEX idx_tx_account_type ON transactions (account_id, type);
