package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.RESOURCES
import com.hypto.iam.server.db.tables.pojos.Resources
import com.hypto.iam.server.db.tables.records.ResourcesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import org.jooq.Result
import org.jooq.impl.DAOImpl

object ResourceRepo : BaseRepo<ResourcesRecord, Resources, String>() {

    private val idFun = fun (resource: Resources): String {
        return resource.hrn
    }

    override suspend fun dao(): DAOImpl<ResourcesRecord, Resources, String> {
        return txMan.getDao(RESOURCES, Resources::class.java, idFun)
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    suspend fun fetchByOrganizationId(value: String): List<Resources> {
        return fetch(RESOURCES.ORGANIZATION_ID, value)
    }

    /**
     * Fetch a unique record that has `hrn = value`
     */
    suspend fun fetchByHrn(hrn: ResourceHrn): ResourcesRecord? {
        val dao = dao()
        return dao.ctx().selectFrom(dao.table).where(RESOURCES.HRN.equal(hrn.toString())).fetchOne()
    }

    suspend fun create(hrn: ResourceHrn, description: String): ResourcesRecord {
        val record = ResourcesRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(hrn.organization)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())
            .setDescription(description)

        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun update(hrn: ResourceHrn, description: String): ResourcesRecord? {
        val condition = RESOURCES.HRN.equal(hrn.toString())
        val dao = dao()
        return dao.ctx().update(dao.table)
            .set(RESOURCES.DESCRIPTION, description)
            .set(RESOURCES.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    suspend fun delete(hrn: ResourceHrn): Boolean {
        val record = ResourcesRecord().setHrn(hrn.toString())
        record.attach(dao().configuration())
        return record.delete() > 0
    }

    suspend fun fetchByOrganizationIdPaginated(
        organizationId: String,
        paginationContext: PaginationContext
    ): Result<ResourcesRecord> {
        val dao = dao()
        return dao.ctx().selectFrom(dao.table)
            .where(RESOURCES.ORGANIZATION_ID.eq(organizationId))
            .paginate(RESOURCES.HRN, paginationContext)
            .fetch()
    }
}
