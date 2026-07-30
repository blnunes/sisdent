CREATE TABLE user_permissions_v7 (
    user_id BIGINT NOT NULL,
    permission ENUM (
        'READ_USERS', 'MAINTAIN_USERS', 'READ_PATIENTS', 'MAINTAIN_PATIENTS',
        'READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES', 'READ_ADDRESSES',
        'MAINTAIN_ADDRESSES', 'READ_COUNTRIES', 'MAINTAIN_COUNTRIES',
        'READ_ADMINISTRATIVE_DIVISIONS', 'MAINTAIN_ADMINISTRATIVE_DIVISIONS',
        'READ_PERMISSIONS', 'MAINTAIN_PERMISSIONS'
    ) NOT NULL,
    PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_user_permissions_user_v7
        FOREIGN KEY (user_id) REFERENCES app_users (id)
);

INSERT INTO user_permissions_v7 (user_id, permission)
SELECT user_id,
       CASE CAST(permission AS VARCHAR)
           WHEN 'READ_STATES' THEN 'READ_ADMINISTRATIVE_DIVISIONS'
           WHEN 'MAINTAIN_STATES' THEN 'MAINTAIN_ADMINISTRATIVE_DIVISIONS'
           ELSE CAST(permission AS VARCHAR)
       END
FROM user_permissions;

DROP TABLE user_permissions;
ALTER TABLE user_permissions_v7 RENAME TO user_permissions;
