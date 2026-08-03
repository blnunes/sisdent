DROP TABLE account_email_claims;
DROP TABLE email_verification_challenges;

ALTER TABLE accounts DROP CONSTRAINT fk_accounts_legacy_user;
ALTER TABLE accounts DROP CONSTRAINT uk_accounts_legacy_user;
ALTER TABLE accounts DROP COLUMN legacy_user_id;
ALTER TABLE accounts DROP COLUMN email_migration_required;
ALTER TABLE accounts DROP COLUMN email_verified;
ALTER TABLE accounts DROP COLUMN pending_email;

DROP TABLE user_permissions;
DROP TABLE app_users;
