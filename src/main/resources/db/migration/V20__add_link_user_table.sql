CREATE TABLE link_users (
  id VARCHAR(512) NOT NULL,
  leader_user VARCHAR(1200) NOT NULL,
  subordinate_user VARCHAR(1200) NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,

  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX leader_subordinate_user_idx ON link_users (leader_user, subordinate_user);