/*Drop existing constraints & recreate it with cascade action on delete*/

ALTER TABLE users
    DROP CONSTRAINT users_organization_id_fkey;
ALTER TABLE users
    ADD CONSTRAINT users_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

ALTER TABLE users_auth_providers
    DROP CONSTRAINT users_auth_providers_user_hrn_fkey;
ALTER TABLE users_auth_providers
    ADD CONSTRAINT  users_auth_providers_user_hrn_fkey FOREIGN KEY (user_hrn) REFERENCES users (hrn) ON DELETE CASCADE;

ALTER TABLE resources
    DROP CONSTRAINT resources_organization_id_fkey;
ALTER TABLE resources
    ADD CONSTRAINT  resources_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

ALTER TABLE actions
    DROP CONSTRAINT actions_resource_hrn_fkey;
ALTER TABLE actions
    ADD CONSTRAINT  actions_resource_hrn_fkey FOREIGN KEY (resource_hrn) REFERENCES resources (hrn) ON DELETE CASCADE;

ALTER TABLE policies
    DROP CONSTRAINT policies_organization_id_fkey;
ALTER TABLE policies
    ADD CONSTRAINT  policies_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

ALTER TABLE user_policies
    DROP CONSTRAINT user_policies_policy_hrn_fkey;
ALTER TABLE user_policies
    ADD CONSTRAINT  user_policies_policy_hrn_fkey FOREIGN KEY (policy_hrn) REFERENCES policies (hrn) ON DELETE CASCADE;