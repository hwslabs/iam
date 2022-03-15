package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Policies.POLICIES
import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.service.DatabaseFactory
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import org.jooq.Result
import org.jooq.impl.DAOImpl

object PoliciesRepo : DAOImpl<PoliciesRecord, Policies, String>(
    POLICIES,
    Policies::class.java,
    DatabaseFactory.getConfiguration()
) {

    override fun getId(policy: Policies): String {
        return policy.hrn
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(value: String): List<Policies> {
        return fetch(POLICIES.ORGANIZATION_ID, value)
    }

    fun fetchByOrganizationIdPaginated(
        organizationId: String,
        paginationContext: PaginationContext
    ): Result<PoliciesRecord> {

        return ctx().selectFrom(table)
            .where(POLICIES.ORGANIZATION_ID.eq(organizationId))
            .paginate(POLICIES.HRN, paginationContext)
            .fetch()
    }

    /**
     * Fetch records that have `hrn = value`
     */
    fun fetchByHrn(hrn: String): PoliciesRecord? {
        return ctx().selectFrom(table).where(POLICIES.HRN.equal(hrn)).fetchOne()
    }

    /**
     * Fetch records that have `organization_id = value, name IN (value, value, ...)`
     */
    fun fetchByHrns(hrns: List<String>): List<Policies> {
        return fetch(POLICIES.HRN, hrns)
    }

    fun create(hrn: ResourceHrn, statements: String): PoliciesRecord {
        val record = PoliciesRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(hrn.organization)
            .setVersion(1)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())
            .setStatements(statements)

        record.attach(configuration())
        record.store()
        return record
    }

    fun deleteByHrn(hrn: String): Boolean {
        val record = PoliciesRecord().setHrn(hrn)
        record.attach(configuration())
        return record.delete() > 0
    }

    fun update(hrn: String, statements: String, version: Int? = null): PoliciesRecord? {
        val condition = POLICIES.HRN.eq(hrn).also { it1 -> version?.let { it1.and(POLICIES.VERSION.eq(it)) } }
        return ctx().update(table)
            .set(POLICIES.STATEMENTS, statements)
            .set(POLICIES.VERSION, POLICIES.VERSION.plus(1))
            .set(POLICIES.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    fun existsByIds(hrns: List<String>): Boolean {
        return ctx()
            .selectCount()
            .from(table)
            .where(POLICIES.HRN.`in`(hrns))
            .fetchOne<Int>(0, Int::class.java) == hrns.size
    }
}
