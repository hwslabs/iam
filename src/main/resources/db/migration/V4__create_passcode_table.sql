CREATE TABLE passcodes (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    valid_until timestamp,
    email VARCHAR(50) NOT NULL,
    organization_id VARCHAR(10),
    type VARCHAR(10) NOT NULL, -- RESET, VERIFY

    created_at timestamp NOT NULL,

    FOREIGN KEY (organization_id) REFERENCES organizations (id)
);