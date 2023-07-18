ALTER TABLE resources
    ADD COLUMN deleted BOOLEAN DEFAULT false;
DROP INDEX IF EXISTS resources_idx_org_id;
CREATE INDEX ON resources (organization_id) WHERE deleted IS FALSE;

ALTER TABLE actions
    ADD COLUMN deleted BOOLEAN DEFAULT false;
DROP INDEX IF EXISTS actions_idx_org_id_resource_hrn;
CREATE INDEX ON actions (organization_id, resource_hrn) WHERE deleted IS FALSE;
