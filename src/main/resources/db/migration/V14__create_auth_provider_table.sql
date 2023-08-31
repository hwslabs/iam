CREATE TABLE auth_provider (
    auth_url        VARCHAR(512) NOT NULL,
    client_id       VARCHAR(512) NOT NULL,
    client_secret   VARCHAR(512) NOT NULL,
    provider        VARCHAR(512) NOT NULL,
    grant_type      VARCHAR(512) NOT NULL,
    scopes          JSONB NOT NULL,
    created_at      timestamp NOT NULL,
    updated_at      timestamp NOT NULL,
    PRIMARY KEY(provider, grant_type)
);