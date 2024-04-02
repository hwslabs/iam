ALTER TABLE policy_templates
ADD COLUMN on_create_org BOOLEAN DEFAULT true NOT NULL;

ALTER TABLE policy_templates
ALTER COLUMN on_create_org DROP DEFAULT;