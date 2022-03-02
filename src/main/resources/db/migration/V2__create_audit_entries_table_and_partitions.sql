create table audit_entries (
  id uuid DEFAULT gen_random_uuid(),
  request_id VARCHAR(200) NOT NULL,
  event_time timestamp NOT NULL,
  principal VARCHAR(200) NOT NULL,
  principal_organization VARCHAR(10) NOT NULL,
  resource VARCHAR(200) NOT NULL,
  operation VARCHAR(200) NOT NULL,
  meta json DEFAULT NULL,
  PRIMARY KEY (id, event_time)
) PARTITION BY RANGE (event_time);

CREATE INDEX audit_entries_idx_principal_organization_principal_event_time ON audit_entries(principal_organization, principal, event_time);
CREATE INDEX audit_entries_idx_resource_event_time ON audit_entries(resource, event_time);

CREATE TABLE audit_entries_y2022m02 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-02-01') TO ('2022-03-01');

CREATE TABLE audit_entries_y2022m03 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-03-01') TO ('2022-04-01');

CREATE TABLE audit_entries_y2022m04 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-04-01') TO ('2022-05-01');

CREATE TABLE audit_entries_y2022m05 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-05-01') TO ('2022-06-01');

CREATE TABLE audit_entries_y2022m06 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-06-01') TO ('2022-07-01');

CREATE TABLE audit_entries_y2022m07 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-07-01') TO ('2022-08-01');

CREATE TABLE audit_entries_y2022m08 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-08-01') TO ('2022-09-01');

CREATE TABLE audit_entries_y2022m09 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-09-01') TO ('2022-10-01');

CREATE TABLE audit_entries_y2022m10 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-10-01') TO ('2022-11-01');

CREATE TABLE audit_entries_y2022m11 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-11-01') TO ('2022-12-01');

CREATE TABLE audit_entries_y2022m12 PARTITION OF audit_entries
    FOR VALUES FROM ('2022-12-01') TO ('2023-01-01');

CREATE TABLE audit_entries_y2023m01 PARTITION OF audit_entries
    FOR VALUES FROM ('2023-01-01') TO ('2023-02-01');
