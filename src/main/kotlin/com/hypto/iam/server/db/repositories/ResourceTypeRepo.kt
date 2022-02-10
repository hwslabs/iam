package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.ResourceTypes
import com.hypto.iam.server.db.tables.records.ResourceTypesRecord
import org.jooq.Record2
import org.jooq.impl.DAOImpl

object ResourceTypeRepo : DAOImpl<ResourceTypesRecord, ResourceTypes, Record2<String, String>>(
    com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES,
    ResourceTypes::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(resourceType: ResourceTypes): Record2<String, String> {
        return compositeKeyRecord(resourceType.organizationId, resourceType.name)
    }

    /**
     * Fetch records that have `organization_id IN (values)`
     */
    fun fetchByOrganizationId(values: String): List<ResourceTypes?>? {
        return fetch(com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES.ORGANIZATION_ID, values)
    }

    /**
     * Fetch a unique record that has `organization_id = value AND name = value`
     */
    fun fetchOneById(orgId: String, resourceName: String): ResourceTypes? {
        return findById(
            ctx().newRecord(
                com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES.ORGANIZATION_ID,
                com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES.NAME
            ).values(orgId, resourceName)
        )
    }
}
