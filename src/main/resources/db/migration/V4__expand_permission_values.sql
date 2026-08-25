CREATE DOMAIN permission_v4 AS VARCHAR(64) CHECK (VALUE IN (
    'READ_USERS',
    'MAINTAIN_USERS',
    'READ_PATIENTS',
    'MAINTAIN_PATIENTS',
    'READ_SPECIALITIES',
    'MAINTAIN_SPECIALITIES',
    'READ_ADDRESSES',
    'MAINTAIN_ADDRESSES',
    'READ_COUNTRIES',
    'MAINTAIN_COUNTRIES',
    'READ_STATES',
    'MAINTAIN_STATES'
));

CREATE TABLE user_permissions_new (
    user_id BIGINT NOT NULL,
    permission permission_v4 NOT NULL,
    PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_user_permissions_user_new
        FOREIGN KEY (user_id) REFERENCES app_users (id)
);

INSERT INTO user_permissions_new (user_id, permission)
WITH target_permissions(permission) AS (
    VALUES
        ('READ_USERS'),
        ('MAINTAIN_USERS'),
        ('READ_PATIENTS'),
        ('MAINTAIN_PATIENTS'),
        ('READ_SPECIALITIES'),
        ('MAINTAIN_SPECIALITIES'),
        ('READ_ADDRESSES'),
        ('MAINTAIN_ADDRESSES'),
        ('READ_COUNTRIES'),
        ('MAINTAIN_COUNTRIES'),
        ('READ_STATES'),
        ('MAINTAIN_STATES')
),
legacy_permissions(user_id, permission) AS (
    SELECT user_id, CAST(permission AS VARCHAR(64))
    FROM user_permissions
)
SELECT source.user_id, target.permission
FROM legacy_permissions source
CROSS JOIN target_permissions target
WHERE source.permission = 'READ' AND target.permission LIKE 'READ_%'
UNION
SELECT source.user_id, target.permission
FROM legacy_permissions source
CROSS JOIN target_permissions target
WHERE target.permission LIKE 'MAINTAIN_%'
  AND (
      (source.permission IN ('CREATE', 'UPDATE', 'DELETE', 'MANAGE_USERS')
          AND target.permission LIKE '%USERS')
      OR (source.permission IN ('CREATE_PATIENTS', 'UPDATE_PATIENTS', 'DELETE_PATIENTS')
          AND target.permission LIKE '%PATIENTS')
      OR (source.permission IN ('CREATE_SPECIALITIES', 'UPDATE_SPECIALITIES', 'DELETE_SPECIALITIES')
          AND target.permission LIKE '%SPECIALITIES')
  )
UNION
SELECT source.user_id, target.permission
FROM legacy_permissions source
JOIN target_permissions target ON target.permission = source.permission
UNION
SELECT admin.id, target.permission
FROM app_users admin
CROSS JOIN target_permissions target
WHERE admin.role = 'ADMIN';

DROP TABLE user_permissions;
ALTER TABLE user_permissions_new RENAME TO user_permissions;
