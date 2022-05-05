ALTER TABLE users
    DROP COLUMN password_hash,
    DROP COLUMN phone,
    DROP COLUMN login_access,
    DROP COLUMN created_by,
    ADD COLUMN verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN deleted BOOLEAN DEFAULT FALSE;

DROP INDEX IF EXISTS users_idx_organization_id_name;
CREATE INDEX users_email_organization_index ON users(email, organization_id);

ALTER TABLE users
   DROP CONSTRAINT users_organization_id_fkey,
   ADD  CONSTRAINT users_organization_id_fkey
   FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;
