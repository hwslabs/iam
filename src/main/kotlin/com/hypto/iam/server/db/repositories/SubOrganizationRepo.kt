package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables
import com.hypto.iam.server.db.tables.records.SubOrganizationsRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.models.SubOrganization
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.impl.DAOImpl

typealias SubOrganizationPk = Record2<String, String>
object SubOrganizationRepo : BaseRepo<SubOrganizationsRecord, SubOrganization, SubOrganizationPk>() {

    private fun getIdFun(dsl: DSLContext): (SubOrganization) -> SubOrganizationPk {
        return fun (subOrganization: SubOrganization): SubOrganizationPk {
            return dsl.newRecord(
                Tables.SUB_ORGANIZATIONS.NAME,
                Tables.SUB_ORGANIZATIONS.ORGANIZATION_ID
            ).values(subOrganization.name, subOrganization.organizationId)
        }
    }

    override suspend fun dao(): DAOImpl<SubOrganizationsRecord, SubOrganization, SubOrganizationPk> {
        return SubOrganizationRepo.txMan.getDao(
            Tables.SUB_ORGANIZATIONS,
            SubOrganization::class.java,
            getIdFun(SubOrganizationRepo.txMan.dsl())
        )
    }

    suspend fun create(
        organizationId: String,
        subOrganizationName: String,
        description: String?
    ): SubOrganizationsRecord {
        val logTimestamp = LocalDateTime.now()
        val record = SubOrganizationsRecord()
            .setOrganizationId(organizationId)
            .setName(subOrganizationName)
            .setDescription(description)
            .setCreatedAt(logTimestamp)
            .setUpdatedAt(logTimestamp)
        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun fetchById(organizationId: String, subOrgName: String): SubOrganizationsRecord? {
        return SubOrganizationRepo
            .ctx("subOrganization.fetchById")
            .selectFrom(Tables.SUB_ORGANIZATIONS)
            .where(
                Tables.SUB_ORGANIZATIONS.ORGANIZATION_ID.eq(organizationId).and(
                    Tables.SUB_ORGANIZATIONS.NAME.eq
                    (subOrgName)
                )
            )
            .fetchOne()
    }

    suspend fun fetchSubOrganizationsPaginated(organizationId: String, context: PaginationContext):
        org.jooq.Result<SubOrganizationsRecord> {
        return SubOrganizationRepo
            .ctx("subOrganization.fetchPaginated")
            .selectFrom(Tables.SUB_ORGANIZATIONS)
            .where(Tables.SUB_ORGANIZATIONS.ORGANIZATION_ID.eq(organizationId))
            .paginate(Tables.SUB_ORGANIZATIONS.NAME, context)
            .fetch()
    }

    suspend fun fetchByOrganizationId(organizationId: String): List<SubOrganizationsRecord?> {
        return SubOrganizationRepo
            .ctx("subOrganization.fetchByOrganizationId")
            .selectFrom(Tables.SUB_ORGANIZATIONS)
            .where(Tables.SUB_ORGANIZATIONS.ORGANIZATION_ID.eq(organizationId))
            .fetch()
    }

    suspend fun update(
        organizationId: String,
        subOrganizationName: String,
        updatedDescription: String?
    ): SubOrganizationsRecord? {
        val logTimestamp = LocalDateTime.now()
        return SubOrganizationRepo
            .ctx("subOrganization.update")
            .update(Tables.SUB_ORGANIZATIONS)
            .apply { updatedDescription?.let { set(Tables.SUB_ORGANIZATIONS.DESCRIPTION, updatedDescription) } }
            .set(Tables.SUB_ORGANIZATIONS.UPDATED_AT, logTimestamp)
            .where(
                Tables.SUB_ORGANIZATIONS.ORGANIZATION_ID.eq(organizationId).and(
                    Tables.SUB_ORGANIZATIONS.NAME.eq
                    (subOrganizationName)
                )
            )
            .returning()
            .fetchOne()
    }

    suspend fun delete(organizationId: String, id: String) {
        SubOrganizationRepo
            .ctx("subOrganization.delete")
            .deleteFrom(Tables.SUB_ORGANIZATIONS)
            .where(
                Tables.SUB_ORGANIZATIONS.ORGANIZATION_ID.eq(organizationId).and(Tables.SUB_ORGANIZATIONS.NAME.eq(id))
            )
            .execute()
    }
}
