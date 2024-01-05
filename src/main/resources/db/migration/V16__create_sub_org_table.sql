-- Sub organizations table
CREATE TABLE sub_organizations (
  name VARCHAR(1200) NOT NULL,
  organization_id VARCHAR(512) NOT NULL,
  description text,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  PRIMARY KEY (name, organization_id),
  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

COMMENT ON TABLE sub_organizations
IS 'Table to store all sub organization details';
COMMENT ON COLUMN sub_organizations.name
IS 'name of the sub organization';
COMMENT ON COLUMN sub_organizations.description
IS 'description of the sub organization';


-- Add sub organization id to the users table
ALTER TABLE users ADD COLUMN sub_organization_name VARCHAR(1200);

-- Add sub organization id to the passcodes table
ALTER TABLE passcodes
    ADD COLUMN sub_organization_name text DEFAULT NULL;

-- Update varchar limit for in user table for hrn and created_by fields
ALTER TABLE users
    ALTER COLUMN hrn TYPE VARCHAR(1300),
    ALTER COLUMN created_by TYPE VARCHAR(1300);