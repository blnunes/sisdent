ALTER TABLE accounts ADD COLUMN preferred_language VARCHAR(5) NOT NULL DEFAULT 'en';
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_preferred_language
    CHECK (preferred_language IN ('pt-PT', 'en', 'nl'));
