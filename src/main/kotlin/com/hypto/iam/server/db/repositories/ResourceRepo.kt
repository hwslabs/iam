package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.RESOURCES
import com.hypto.iam.server.db.tables.pojos.Resources
import com.hypto.iam.server.db.tables.records.ResourcesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.service.DatabaseFactory
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import org.jooq.Result
import org.jooq.impl.DAOImpl

object ResourceRepo : DAOImpl<ResourcesRecord, Resources, String>(
    RESOURCES,
    Resources::class.java,
    DatabaseFactory.getConfiguration()
) {

    override fun getId(resource: Resources): String {
        return resource.hrn
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(value: String): List<Resources?> {
        return fetch(RESOURCES.ORGANIZATION_ID, value)
    }

    /**
     * Fetch a unique record that has `hrn = value`
     */
    fun fetchByHrn(hrn: ResourceHrn): ResourcesRecord? {
        return ctx().selectFrom(table).where(RESOURCES.HRN.equal(hrn.toString())).fetchOne()
    }

    fun create(hrn: ResourceHrn, description: String): ResourcesRecord {
        val record = ResourcesRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(hrn.organization)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())
            .setDescription(description)

        record.attach(CredentialsRepo.configuration())
        record.store()
        return record
    }

    fun update(hrn: ResourceHrn, description: String): ResourcesRecord? {
        val condition = RESOURCES.HRN.equal(hrn.toString())
        return ctx().update(table)
            .set(RESOURCES.DESCRIPTION, description)
            .set(RESOURCES.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    fun delete(hrn: ResourceHrn): Boolean {
        val record = ResourcesRecord().setHrn(hrn.toString())
        record.attach(configuration())
        return record.delete() > 0
    }

    fun fetchByOrganizationIdPaginated(
        organizationId: String,
        paginationContext: PaginationContext
    ): Result<ResourcesRecord> {
        return ctx().selectFrom(table)
            .where(RESOURCES.ORGANIZATION_ID.eq(organizationId))
            .paginate(RESOURCES.HRN, paginationContext)
            .fetch()
    }
}
