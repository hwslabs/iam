ALTER TABLE resources
    ADD COLUMN deleted BOOLEAN DEFAULT false;
CREATE INDEX resources_deleted ON resources(deleted);

ALTER TABLE actions
    ADD COLUMN deleted BOOLEAN DEFAULT false;
CREATE INDEX actions_deleted ON actions(deleted);
