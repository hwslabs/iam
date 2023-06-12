CREATE TABLE policy_templates (
    name VARCHAR(512) PRIMARY KEY,
    status VARCHAR(512) NOT NULL, -- ACTIVE, ARCHIVED
    is_root_policy BOOLEAN NOT NULL,
    statements text NOT NULL,

    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);



INSERT INTO policy_templates values (
    'admin',
    'ACTIVE',
    TRUE,
    'p, hrn:{{organization_id}}::iam-policy/admin, ^hrn:{{organization_id}}$, hrn:{{organization_id}}:*, allow
p, hrn:{{organization_id}}::iam-policy/admin, ^hrn:{{organization_id}}::*, hrn:{{organization_id}}::*, allow
',
    'NOW()',
    'NOW()'
);
