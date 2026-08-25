ALTER TABLE accounts ADD COLUMN account_management_organization_id BIGINT;
ALTER TABLE accounts ADD CONSTRAINT fk_accounts_management_organization
    FOREIGN KEY (account_management_organization_id) REFERENCES organizations (id);

MERGE INTO accounts account
USING (
    SELECT account_id, MIN(organization_id) AS organization_id
    FROM memberships
    WHERE active
      AND clinic_unit_id IS NULL
      AND role = 'ORGANIZATION_ADMIN'
    GROUP BY account_id
) scope ON (scope.account_id = account.id)
WHEN MATCHED THEN UPDATE SET
    account_management_organization_id = scope.organization_id;

CREATE INDEX idx_accounts_management_organization ON accounts (account_management_organization_id);
