CREATE TABLE user_permissions_v5 (
    user_id BIGINT NOT NULL,
    permission ENUM (
        'READ_USERS', 'MAINTAIN_USERS', 'READ_PATIENTS', 'MAINTAIN_PATIENTS',
        'READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES', 'READ_ADDRESSES',
        'MAINTAIN_ADDRESSES', 'READ_COUNTRIES', 'MAINTAIN_COUNTRIES',
        'READ_STATES', 'MAINTAIN_STATES', 'READ_PERMISSIONS', 'MAINTAIN_PERMISSIONS'
    ) NOT NULL,
    PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_user_permissions_user_v5 FOREIGN KEY (user_id) REFERENCES app_users (id)
);

INSERT INTO user_permissions_v5 (user_id, permission)
SELECT user_id, permission FROM user_permissions;

INSERT INTO user_permissions_v5 (user_id, permission)
SELECT id, 'READ_PERMISSIONS' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_PERMISSIONS' FROM app_users WHERE role = 'ADMIN';

DROP TABLE user_permissions;
ALTER TABLE user_permissions_v5 RENAME TO user_permissions;
