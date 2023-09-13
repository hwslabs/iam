-- Authorization methods of User Table
CREATE TABLE user_auth (
    user_hrn        VARCHAR(512) NOT NULL,
    provider_name   VARCHAR(512) NOT NULL,
    auth_metadata   JSONB,
    created_at      timestamp NOT NULL,
    updated_at      timestamp NOT NULL,
    PRIMARY KEY(user_hrn, provider_name)
);

COMMENT ON TABLE user_auth
IS 'Authorization methods of User Table';
COMMENT ON COLUMN user_auth.user_hrn
IS 'User HRN';
COMMENT ON COLUMN user_auth.provider_name
IS 'OAuth Provider Name (e.g. Google, Microsoft)';
COMMENT ON COLUMN user_auth.auth_metadata
IS 'OAuth Authorization Metadata (e.g. access_token, refresh_token, token_type, expires_in, scope)';

CREATE INDEX user_auth_idx_user_hrn ON user_auth(user_hrn);