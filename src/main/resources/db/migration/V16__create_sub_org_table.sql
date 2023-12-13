-- Sub organizations table
CREATE TABLE sub_organizations (
  id VARCHAR(512) NOT NULL,
  name VARCHAR(512) NOT NULL,
  organization_id VARCHAR(512) NOT NULL,
  description text,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  PRIMARY KEY (id, organization_id),
  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

COMMENT ON TABLE sub_organizations
IS 'Table to store all sub organization details';
COMMENT ON COLUMN sub_organizations.id
IS 'unique id of the sub organization under an organization';
COMMENT ON COLUMN sub_organizations.name
IS 'name of the sub organization';
COMMENT ON COLUMN sub_organizations.description
IS 'description of the sub organization';


-- Add sub organization id to the users table
ALTER TABLE users ADD COLUMN sub_organization_id VARCHAR(512);

-- Add sub organization id to the passcodes table
ALTER TABLE passcodes
    ADD COLUMN sub_organization_id text DEFAULT NULL;