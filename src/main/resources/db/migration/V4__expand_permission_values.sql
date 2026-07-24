CREATE TABLE user_permissions_new (
    user_id BIGINT NOT NULL,
    permission ENUM (
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
    ) NOT NULL,
    PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_user_permissions_user_new
        FOREIGN KEY (user_id) REFERENCES app_users (id)
);

INSERT INTO user_permissions_new (user_id, permission)
SELECT user_id, 'READ_USERS' FROM user_permissions WHERE permission = 'READ'
UNION
SELECT user_id, 'READ_PATIENTS' FROM user_permissions WHERE permission = 'READ'
UNION
SELECT user_id, 'READ_SPECIALITIES' FROM user_permissions WHERE permission = 'READ'
UNION
SELECT user_id, 'READ_ADDRESSES' FROM user_permissions WHERE permission = 'READ'
UNION
SELECT user_id, 'READ_COUNTRIES' FROM user_permissions WHERE permission = 'READ'
UNION
SELECT user_id, 'READ_STATES' FROM user_permissions WHERE permission = 'READ'
UNION
SELECT user_id, 'MAINTAIN_USERS' FROM user_permissions WHERE permission IN ('CREATE', 'UPDATE', 'DELETE', 'MANAGE_USERS')
UNION
SELECT user_id, 'MAINTAIN_PATIENTS' FROM user_permissions WHERE permission IN ('CREATE_PATIENTS', 'UPDATE_PATIENTS', 'DELETE_PATIENTS')
UNION
SELECT user_id, 'MAINTAIN_SPECIALITIES' FROM user_permissions WHERE permission IN ('CREATE_SPECIALITIES', 'UPDATE_SPECIALITIES', 'DELETE_SPECIALITIES')
UNION
SELECT user_id, 'READ_USERS' FROM user_permissions WHERE permission = 'READ_USERS'
UNION
SELECT user_id, 'READ_PATIENTS' FROM user_permissions WHERE permission = 'READ_PATIENTS'
UNION
SELECT user_id, 'READ_SPECIALITIES' FROM user_permissions WHERE permission = 'READ_SPECIALITIES'
UNION
SELECT user_id, 'READ_ADDRESSES' FROM user_permissions WHERE permission = 'READ_ADDRESSES'
UNION
SELECT user_id, 'READ_COUNTRIES' FROM user_permissions WHERE permission = 'READ_COUNTRIES'
UNION
SELECT user_id, 'READ_STATES' FROM user_permissions WHERE permission = 'READ_STATES'
UNION
SELECT id, 'READ_USERS' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_USERS' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'READ_PATIENTS' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_PATIENTS' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'READ_SPECIALITIES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_SPECIALITIES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'READ_ADDRESSES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_ADDRESSES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'READ_COUNTRIES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_COUNTRIES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'READ_STATES' FROM app_users WHERE role = 'ADMIN'
UNION
SELECT id, 'MAINTAIN_STATES' FROM app_users WHERE role = 'ADMIN';

DROP TABLE user_permissions;
ALTER TABLE user_permissions_new RENAME TO user_permissions;
