CREATE TABLE passcodes (
    id VARCHAR(10) PRIMARY KEY,
    valid_until timestamp NOT NULL,
    email VARCHAR(50) NOT NULL,
    organization_id VARCHAR(10),
    purpose VARCHAR(10) NOT NULL, -- RESET, VERIFY

    created_at timestamp NOT NULL,

    FOREIGN KEY (organization_id) REFERENCES organizations (id)
);