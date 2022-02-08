CREATE TABLE organizations (
  id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  admin_user VARCHAR(15),
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL

);

CREATE TABLE users (
  id uuid PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
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

CREATE INDEX users_idx_organization_id_name ON users(organization_id, name);

CREATE TABLE users_auth_providers (
--   Supports 400 million users each with 5 auth providers
  id SERIAL PRIMARY KEY,
  user_id uuid NOT NULL,
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
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE credentials (
  id uuid PRIMARY KEY,
  valid_until timestamp,
  status VARCHAR(10) NOT NULL,
  refresh_token text,
  user_id uuid NOT NULL,

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX credentials_idx_user_id_refresh_token ON credentials(user_id, refresh_token);

CREATE TABLE resource_types (
  organization_id VARCHAR(10) NOT NULL,
  name VARCHAR(50) NOT NULL,
  description text,

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  PRIMARY KEY (organization_id, name),
  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE actions (
  organization_id VARCHAR(10) NOT NULL,
  name VARCHAR(50) NOT NULL,
  resource_type VARCHAR(50) NOT NULL,
  description text,

  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  PRIMARY KEY (organization_id, resource_type, name),
  FOREIGN KEY (organization_id, resource_type) REFERENCES resource_types (organization_id, name)
);

CREATE TABLE policies (
  organization_id VARCHAR(10) NOT NULL,
  name VARCHAR(50) NOT NULL,
  statements JSONB NOT NULL,

  PRIMARY KEY (organization_id, name),
  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE user_policies (
  id uuid PRIMARY KEY,
  principal_hrn VARCHAR(100) NOT NULL,
  policy_hrn VARCHAR(100) NOT NULL
);
CREATE INDEX user_policies_idx_principal ON user_policies(principal_hrn);

-- List all permissions a user has on a resource+action:
-- 1. Get user_policies for principal_hrn of user as A
-- 2. Get user_policies for principal_hrn of resource as B
-- 3. Filter policy statements in B with condition: statement_principal = user && statement_action = action




