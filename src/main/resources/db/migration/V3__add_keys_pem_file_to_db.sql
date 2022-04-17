ALTER TABLE master_keys
    RENAME private_key TO private_key_der;

ALTER TABLE master_keys
    RENAME public_key TO public_key_der;

ALTER TABLE master_keys
    ADD COLUMN private_key_pem BYTEA,
    ADD COLUMN public_key_pem BYTEA;