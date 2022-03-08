CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE organizations (
  id VARCHAR(10) PRIMARY KEY, -- 10 char alphabets (upper case only)
  name VARCHAR(50) NOT NULL,
  description text NOT NULL,
  admin_user VARCHAR(15),

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL
);

CREATE TABLE users (
  hrn VARCHAR(200) PRIMARY KEY, -- username is a part of this hrn
  password_hash text NOT NULL,
  email VARCHAR(50) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  login_access boolean DEFAULT FALSE,
  user_type VARCHAR(10) NOT NULL,
  status VARCHAR(10) NOT NULL,
  created_by uuid NOT NULL,
  organization_id VARCHAR(10),

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX users_idx_organization_id_name ON users(organization_id);

CREATE TABLE users_auth_providers (
--   Supports 400 million users each with 5 auth providers
  id SERIAL PRIMARY KEY,
  user_hrn VARCHAR(200) NOT NULL,
  organization_id VARCHAR(10),
  auth_provider VARCHAR(10) NOT NULL,
  protocol VARCHAR(10) NOT NULL,
  valid_until timestamp,
  data_field_1 text,
  data_field_1_type VARCHAR(30),
  data_field_2 text,
  data_field_2_type VARCHAR(30),

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  -- PRIMARY KEY (user_id, auth_provider, protocol),
  FOREIGN KEY (user_hrn) REFERENCES users (hrn)
);

CREATE TABLE credentials (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  valid_until timestamp,
  status VARCHAR(10) NOT NULL,
  refresh_token text,
  user_hrn VARCHAR(200) NOT NULL,

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  FOREIGN KEY (user_hrn) REFERENCES users (hrn)
);
CREATE INDEX credentials_idx_user_hrn ON credentials(user_hrn);
CREATE INDEX credentials_idx_refresh_token ON credentials(refresh_token);

CREATE TABLE resources (
  hrn VARCHAR(200) PRIMARY KEY, -- resourceName is part of this hrn
  organization_id VARCHAR(10) NOT NULL,
  description text NOT NULL,

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);
CREATE INDEX resources_idx_org_id ON resources(organization_id);

CREATE TABLE actions (
  hrn VARCHAR(200) PRIMARY KEY, -- actionName is part of this hrn
  organization_id VARCHAR(10) NOT NULL,
  resource_hrn VARCHAR(200) NOT NULL,
  description text NOT NULL,

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  FOREIGN KEY (resource_hrn) REFERENCES resources (hrn)
);
CREATE INDEX actions_idx_org_id_resource_hrn ON actions(organization_id, resource_hrn);

CREATE TABLE policies (
  hrn VARCHAR(200) PRIMARY KEY, -- policyName is part of this hrn
  organization_id VARCHAR(10) NOT NULL,
  version INT NOT NULL,
  statements text NOT NULL, -- Supports max string of 1GB (https://stackoverflow.com/a/39966079)

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);
CREATE INDEX policies_idx_org_id ON policies(organization_id);

-- user_policy entry always belongs to the organization to which the principal blongs to.
CREATE TABLE user_policies (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  principal_hrn VARCHAR(200) NOT NULL,
  policy_hrn VARCHAR(200) NOT NULL,

  created_at timestamp NOT NULL,

  FOREIGN KEY (policy_hrn) REFERENCES policies (hrn)
);
CREATE UNIQUE INDEX user_policies_idx_principal_policy ON user_policies(principal_hrn, policy_hrn);

-- List all permissions a user has on a resource+action:
-- 1. Get user_policies for principal_hrn of user as A
-- 2. Get user_policies for principal_hrn of resource as B
-- 3. Filter policy statements in B with condition: statement_principal = user && statement_action = action

CREATE TABLE master_keys (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  private_key BYTEA NOT NULL,
  public_key BYTEA NOT NULL,
  status VARCHAR(10) NOT NULL, -- SIGNING, VERIFYING, EXPIRED

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL
);

CREATE INDEX ec_keys_idx_status ON master_keys(status) WHERE status IN ('SIGNING', 'VERIFYING');




