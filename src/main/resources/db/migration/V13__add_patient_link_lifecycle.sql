CREATE INDEX idx_patient_links_scope_active
    ON patient_organization_links (organization_id, clinic_unit_id, active);
