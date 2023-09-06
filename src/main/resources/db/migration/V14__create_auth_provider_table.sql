-- OAuth Grant Type Enum
CREATE TYPE grant_type AS ENUM (
    'CODE_AUTHORIZATION',
    'CUSTOM',
    'IMPLICIT'
);

-- OAuth Provider Table
CREATE TABLE auth_provider (
    provider_name    VARCHAR(512) NOT NULL PRIMARY KEY, -- OAuth Provider Name (e.g. Google, Microsoft)
    auth_url        VARCHAR(512) NOT NULL,              -- OAuth Authorization URL that frontend consumes to redirect user
    client_id       VARCHAR(512) NOT NULL,              -- OAuth Client ID generated at OAuth Provider to include in request
    client_secret   VARCHAR(512) NOT NULL,              -- OAuth Client Secret generated at OAuth Provider to obtain refresh token
    created_at      timestamp NOT NULL,
    updated_at      timestamp NOT NULL
);