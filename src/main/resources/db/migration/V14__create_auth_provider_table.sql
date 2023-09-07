-- OAuth Grant Type Enum
CREATE TYPE grant_type AS ENUM (
    'CODE_AUTHORIZATION',
    'CUSTOM',
    'IMPLICIT'
);

-- OAuth Provider Table
CREATE TABLE auth_provider (
    provider_name   VARCHAR(512) NOT NULL PRIMARY KEY,
    auth_url        VARCHAR(512) NOT NULL,
    client_id       VARCHAR(512) NOT NULL,
    client_secret   VARCHAR(512) NOT NULL,
    created_at      timestamp NOT NULL,
    updated_at      timestamp NOT NULL
);

COMMENT ON TABLE auth_provider
IS 'OAuth Provider Table';
COMMENT ON COLUMN auth_provider.provider_name
IS 'OAuth Provider Name (e.g. Google, Microsoft)';
COMMENT ON COLUMN auth_provider.auth_url
IS 'OAuth Authorization URL that frontend consumes to redirect user';
COMMENT ON COLUMN auth_provider.client_id
IS 'OAuth Client ID generated at OAuth Provider to include in request';
COMMENT ON COLUMN auth_provider.client_secret
IS 'OAuth Client Secret generated at OAuth Provider to obtain refresh token';