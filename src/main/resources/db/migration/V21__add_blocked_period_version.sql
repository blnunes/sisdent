ALTER TABLE appointment_blocked_periods
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
