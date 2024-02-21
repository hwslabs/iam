ALTER TABLE policy_templates
    ADD COLUMN required_variables text[] not null default '{}';