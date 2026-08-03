ALTER TABLE accounts ADD COLUMN account_management_organization_id BIGINT;
ALTER TABLE accounts ADD CONSTRAINT fk_accounts_management_organization
    FOREIGN KEY (account_management_organization_id) REFERENCES organizations (id);

UPDATE accounts a
SET account_management_organization_id = (
    SELECT MIN(m.organization_id)
    FROM memberships m
    WHERE m.account_id = a.id
      AND m.active = TRUE
      AND m.clinic_unit_id IS NULL
      AND m.role = 'ORGANIZATION_ADMIN'
)
WHERE EXISTS (
    SELECT 1
    FROM memberships m
    WHERE m.account_id = a.id
      AND m.active = TRUE
      AND m.clinic_unit_id IS NULL
      AND m.role = 'ORGANIZATION_ADMIN'
);

CREATE INDEX idx_accounts_management_organization ON accounts (account_management_organization_id);
