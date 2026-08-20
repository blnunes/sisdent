ALTER TABLE accounts ADD COLUMN avatar_key VARCHAR(160);
ALTER TABLE accounts ADD COLUMN avatar_content_type VARCHAR(80);
ALTER TABLE accounts ADD COLUMN avatar_updated_at TIMESTAMP WITH TIME ZONE;
