package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.ResourceTypes
import com.hypto.iam.server.db.tables.records.ResourceTypesRecord
import org.jooq.impl.DAOImpl

object ResourceTypeRepo : DAOImpl<ResourceTypesRecord, ResourceTypes, String>(
    com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES,
    ResourceTypes::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(resourceType: ResourceTypes): String {
        return resourceType.hrn
    }

    /**
     * Fetch records that have `organization_id IN (values)`
     */
    fun fetchByOrganizationId(values: String): List<ResourceTypes?>? {
        return fetch(com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES.ORGANIZATION_ID, values)
    }

    /**
     * Fetch a unique record that has `hrn = value`
     */
    fun fetchOneByHrn(hrn: String): ResourceTypes? {
        return fetchOne(com.hypto.iam.server.db.tables.ResourceTypes.RESOURCE_TYPES.HRN, hrn)
    }
}
