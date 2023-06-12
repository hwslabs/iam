package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Policies.POLICIES
import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import org.jooq.Result
import org.jooq.impl.DAOImpl

data class RawPolicyPayload(val hrn: ResourceHrn, val statements: String)

object PoliciesRepo : BaseRepo<PoliciesRecord, Policies, String>() {

    private val idFun = fun (policy: Policies) = policy.hrn

    override suspend fun dao(): DAOImpl<PoliciesRecord, Policies, String> =
        txMan.getDao(POLICIES, Policies::class.java, idFun)

    /**
     * Fetch records that have `organization_id = value`
     */
    suspend fun fetchByOrganizationId(value: String): List<Policies> =
        dao().fetch(POLICIES.ORGANIZATION_ID, value)

    suspend fun fetchByOrganizationIdPaginated(
        organizationId: String,
        paginationContext: PaginationContext
    ): Result<PoliciesRecord> = ctx("policies.fetchPaginated")
        .selectFrom(POLICIES)
        .where(POLICIES.ORGANIZATION_ID.eq(organizationId))
        .paginate(POLICIES.HRN, paginationContext)
        .fetch()

    /**
     * Fetch records that have `hrn = value`
     */
    suspend fun fetchByHrn(hrn: String): PoliciesRecord? =
        ctx("policies.fetchByHrn").selectFrom(POLICIES).where(POLICIES.HRN.equal(hrn)).fetchOne()

    suspend fun fetchByHrns(hrns: List<String>): List<PoliciesRecord> =
        ctx("policies.fetchByHrns").selectFrom(POLICIES).where(POLICIES.HRN.`in`(hrns)).fetch()

    suspend fun create(hrn: ResourceHrn, statements: String): PoliciesRecord {
        val record = PoliciesRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(hrn.organization)
            .setVersion(1)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())
            .setStatements(statements)

        record.attach(txMan.configuration())
        record.store()
        return record
    }

    suspend fun batchCreate(rawPolicyPayloads: List<RawPolicyPayload>): List<PoliciesRecord> {
        val policiesToCreate = rawPolicyPayloads.map {
            PoliciesRecord(
                it.hrn.toString(),
                it.hrn.organization,
                1,
                it.statements,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        }
        return insertBatch("policies.batchCreate", policiesToCreate)
    }

    suspend fun deleteByHrn(hrn: String): Boolean {
        val record = PoliciesRecord().setHrn(hrn)
        record.attach(txMan.configuration())
        return record.delete() > 0
    }

    suspend fun update(hrn: String, statements: String, version: Int? = null): PoliciesRecord? {
        val condition = POLICIES.HRN.eq(hrn).also { it1 -> version?.let { it1.and(POLICIES.VERSION.eq(it)) } }
        return ctx("policies.update")
            .update(POLICIES)
            .set(POLICIES.STATEMENTS, statements)
            .set(POLICIES.VERSION, POLICIES.VERSION.plus(1))
            .set(POLICIES.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    suspend fun existsByIds(hrns: List<String>): Boolean {
        return ctx("policies.countByIds")
            .selectCount()
            .from(POLICIES)
            .where(POLICIES.HRN.`in`(hrns))
            .fetchOne(0, Int::class.java) == hrns.size
    }
}
