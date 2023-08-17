ALTER TABLE policy_templates ADD COLUMN description VARCHAR(512) NULL;

ALTER TABLE policies ADD COLUMN description VARCHAR(512) NULL;
