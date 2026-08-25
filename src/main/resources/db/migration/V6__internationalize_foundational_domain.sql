CREATE DOMAIN migration_v6_actor AS VARCHAR(255) DEFAULT 'system';

ALTER TABLE states ADD COLUMN country_id BIGINT;
UPDATE states
SET country_id = (SELECT id FROM countries WHERE code = 'US')
WHERE country_id IS NULL;
ALTER TABLE states ALTER COLUMN country_id SET NOT NULL;
ALTER TABLE states ADD CONSTRAINT fk_states_country
    FOREIGN KEY (country_id) REFERENCES countries (id);
ALTER TABLE states DROP CONSTRAINT uk_states_abbreviation;
ALTER TABLE states ALTER COLUMN abbreviation VARCHAR(32);
ALTER TABLE states ADD COLUMN division_type VARCHAR(32) NOT NULL DEFAULT 'STATE';
ALTER TABLE states ADD CONSTRAINT uk_states_country_abbreviation
    UNIQUE (country_id, abbreviation);

ALTER TABLE states RENAME TO administrative_divisions;
ALTER TABLE addresses RENAME COLUMN state_id TO administrative_division_id;
ALTER TABLE addresses ALTER COLUMN administrative_division_id DROP NOT NULL;

ALTER TABLE addresses DROP CONSTRAINT uk_addresses_postal_code;
ALTER TABLE addresses ALTER COLUMN postal_code VARCHAR(20);
ALTER TABLE addresses ALTER COLUMN postal_code DROP NOT NULL;
ALTER TABLE addresses ALTER COLUMN district DROP NOT NULL;
ALTER TABLE addresses ADD COLUMN city VARCHAR(255);
UPDATE addresses SET city = district WHERE city IS NULL;
ALTER TABLE addresses ALTER COLUMN city SET NOT NULL;

ALTER TABLE patients DROP CONSTRAINT uk_patients_tax_id;
ALTER TABLE patients ALTER COLUMN tax_id DROP NOT NULL;
ALTER TABLE patients DROP CONSTRAINT uk_patients_identification_number;
ALTER TABLE patients ADD COLUMN global_id UUID DEFAULT RANDOM_UUID() NOT NULL;
ALTER TABLE patients ADD CONSTRAINT uk_patients_global_id UNIQUE (global_id);
ALTER TABLE patients ADD COLUMN document_issuer_country_id BIGINT;
UPDATE patients
SET document_issuer_country_id = nationality_country_id
WHERE document_issuer_country_id IS NULL;
ALTER TABLE patients ALTER COLUMN document_issuer_country_id SET NOT NULL;
ALTER TABLE patients ADD CONSTRAINT fk_patients_document_issuer_country
    FOREIGN KEY (document_issuer_country_id) REFERENCES countries (id);

ALTER TABLE patients ADD COLUMN document_type VARCHAR(32);
UPDATE patients
SET document_type = CASE
    WHEN identification_type = 'NATIONAL_ID' THEN 'NATIONAL_ID_CARD'
    ELSE 'PASSPORT'
END
WHERE document_type IS NULL;
ALTER TABLE patients ALTER COLUMN document_type SET NOT NULL;
ALTER TABLE patients DROP COLUMN identification_type;
ALTER TABLE patients ALTER COLUMN document_type RENAME TO identification_type;
ALTER TABLE patients ADD CONSTRAINT uk_patients_issued_document
    UNIQUE (identification_type, document_issuer_country_id, identification_number);

ALTER TABLE procedures RENAME TO dental_procedures;
ALTER TABLE specialities ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE dental_procedures ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE countries ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE countries ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE countries ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE countries ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE countries ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE administrative_divisions ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE administrative_divisions ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE administrative_divisions ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE administrative_divisions ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE administrative_divisions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE addresses ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE addresses ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE addresses ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE addresses ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE addresses ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE patients ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE patients ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE patients ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE patients ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE patients ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE specialities ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE specialities ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE specialities ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE specialities ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE specialities ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE dental_procedures ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dental_procedures ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE dental_procedures ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dental_procedures ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE dental_procedures ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE app_users ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_users ADD COLUMN created_by migration_v6_actor NOT NULL;
ALTER TABLE app_users ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_users ADD COLUMN updated_by migration_v6_actor NOT NULL;
ALTER TABLE app_users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_administrative_divisions_country
    ON administrative_divisions (country_id);
CREATE INDEX idx_addresses_country_postal_code
    ON addresses (country_id, postal_code);
CREATE INDEX idx_addresses_administrative_division
    ON addresses (administrative_division_id);
CREATE INDEX idx_patients_global_id ON patients (global_id);
CREATE INDEX idx_patients_document
    ON patients (document_issuer_country_id, identification_type, identification_number);
CREATE INDEX idx_dental_procedures_speciality_status
    ON dental_procedures (speciality_id, status);
