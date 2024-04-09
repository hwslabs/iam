ALTER TABLE passcodes
    ADD COLUMN last_sent TIMESTAMP;

UPDATE passcodes
    SET last_sent = created_at;

ALTER TABLE passcodes
    ALTER COLUMN last_sent SET NOT NULL;