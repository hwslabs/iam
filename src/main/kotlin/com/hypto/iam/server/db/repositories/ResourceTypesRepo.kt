package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.RESOURCE_TYPES
import com.hypto.iam.server.db.tables.pojos.ResourceTypes
import com.hypto.iam.server.db.tables.records.ResourceTypesRecord
import com.hypto.iam.server.service.DatabaseFactory
import com.hypto.iam.server.utils.GlobalHrn
import java.time.LocalDateTime
import org.jooq.impl.DAOImpl

object ResourceTypesRepo : DAOImpl<ResourceTypesRecord, ResourceTypes, String>(
    RESOURCE_TYPES,
    ResourceTypes::class.java,
    DatabaseFactory.getConfiguration()
) {

    override fun getId(resourceType: ResourceTypes): String {
        return resourceType.hrn
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(value: String): List<ResourceTypes?> {
        return fetch(RESOURCE_TYPES.ORGANIZATION_ID, value)
    }

    /**
     * Fetch a unique record that has `hrn = value`
     */
    fun fetchByHrn(hrn: GlobalHrn): ResourceTypesRecord? {
        return ctx().selectFrom(table).where(RESOURCE_TYPES.HRN.equal(hrn.toString())).fetchOne()
    }

    fun create(hrn: GlobalHrn, description: String): ResourceTypesRecord {
        val record = ResourceTypesRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(hrn.organization)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())
            .setDescription(description)

        record.attach(CredentialsRepo.configuration())
        record.store()
        return record
    }

    fun update(hrn: GlobalHrn, description: String): ResourceTypesRecord? {
        val condition = RESOURCE_TYPES.HRN.equal(hrn.toString())
        return ctx().update(table)
            .set(RESOURCE_TYPES.DESCRIPTION, description)
            .set(RESOURCE_TYPES.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    fun delete(hrn: GlobalHrn): Boolean {
        val record = ResourceTypesRecord().setHrn(hrn.toString())
        record.attach(configuration())
        return record.delete() > 0
    }
}
