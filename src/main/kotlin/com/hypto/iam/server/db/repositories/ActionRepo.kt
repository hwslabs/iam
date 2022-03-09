package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.ACTIONS
import com.hypto.iam.server.db.tables.pojos.Actions
import com.hypto.iam.server.db.tables.records.ActionsRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.utils.GlobalHrn
import java.time.LocalDateTime
import org.jooq.Result
import org.jooq.impl.DAOImpl

object ActionRepo : DAOImpl<ActionsRecord, Actions, String>(
    com.hypto.iam.server.db.tables.Actions.ACTIONS,
    Actions::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(action: Actions): String {
        return action.hrn
    }

    fun fetchByHrn(hrn: GlobalHrn): ActionsRecord? {
        return ctx().selectFrom(table).where(ACTIONS.HRN.eq(hrn.toString())).fetchOne()
    }

    fun create(orgId: String, resourceHrn: GlobalHrn, hrn: GlobalHrn, description: String?): ActionsRecord {
        val record = ActionsRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(orgId)
            .setResourceHrn(resourceHrn.toString())
            .setDescription(description)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())

        record.attach(CredentialsRepo.configuration())
        record.store()
        return record
    }

    fun update(hrn: GlobalHrn, description: String): ActionsRecord? {
        val condition = ACTIONS.HRN.eq(hrn.toString())
        return ctx().update(table)
            .set(ACTIONS.DESCRIPTION, description)
            .set(ACTIONS.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    fun fetchActionsPaginated(
        organizationId: String,
        resourceHrn: GlobalHrn,
        paginationContext: PaginationContext
    ): Result<ActionsRecord> {
        return ctx().selectFrom(table)
            .where(ACTIONS.ORGANIZATION_ID.eq(organizationId).and(ACTIONS.RESOURCE_HRN.eq(resourceHrn.toString())))
            .paginate(ACTIONS.HRN, paginationContext)
            .fetch()
    }

    fun delete(hrn: GlobalHrn): Boolean {
        val record = ActionsRecord().setHrn(hrn.toString())
        record.attach(configuration())
        return record.delete() > 0
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(orgId: String): List<Actions> {
        return fetch(com.hypto.iam.server.db.tables.Actions.ACTIONS.ORGANIZATION_ID, orgId)
    }

    /**
     * Fetch records that have `organization_id = value AND resource = value`
     */
    fun fetchByHrn(orgId: String, resourceHrn: String): List<Actions> {
        return ctx()
            .selectFrom(table)
            .where(
                com.hypto.iam.server.db.tables.Actions.ACTIONS.ORGANIZATION_ID.equal(orgId),
                com.hypto.iam.server.db.tables.Actions.ACTIONS.RESOURCE_HRN.equal(resourceHrn)
            )
            .fetch(mapper())
    }
}
