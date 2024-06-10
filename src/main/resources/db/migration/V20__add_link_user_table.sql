CREATE TABLE link_users (
  id VARCHAR(512) NOT NULL,
  organization_id VARCHAR(512) NOT NULL,
  master_user VARCHAR(1200) NOT NULL,
  subordinate_user VARCHAR(1200) NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  PRIMARY KEY (id),
  FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE UNIQUE INDEX master_subordinate_user_idx ON link_users (master_user, subordinate_user);