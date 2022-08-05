package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.ACTIONS
import com.hypto.iam.server.db.tables.pojos.Actions
import com.hypto.iam.server.db.tables.records.ActionsRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import org.jooq.Result
import org.jooq.impl.DAOImpl

object ActionRepo : BaseRepo<ActionsRecord, Actions, String>() {

    private val idFun = fun (action: Actions): String {
        return action.hrn
    }

    override suspend fun dao(): DAOImpl<ActionsRecord, Actions, String> {
        return txMan.getDao(com.hypto.iam.server.db.tables.Actions.ACTIONS, Actions::class.java, idFun)
    }

    suspend fun fetchByHrn(hrn: ActionHrn): ActionsRecord? {
        return ctx("action.findByHrn").selectFrom(ACTIONS).where(ACTIONS.HRN.eq(hrn.toString())).fetchOne()
    }

    suspend fun create(orgId: String, resourceHrn: ResourceHrn, hrn: ActionHrn, description: String?): ActionsRecord {
        val record = ActionsRecord()
            .setHrn(hrn.toString())
            .setOrganizationId(orgId)
            .setResourceHrn(resourceHrn.toString())
            .setDescription(description)
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now())

        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun update(hrn: ActionHrn, description: String): ActionsRecord? {
        val condition = ACTIONS.HRN.eq(hrn.toString())
        return ctx("action.update").update(ACTIONS)
            .set(ACTIONS.DESCRIPTION, description)
            .set(ACTIONS.UPDATED_AT, LocalDateTime.now())
            .where(condition)
            .returning()
            .fetchOne()
    }

    suspend fun fetchActionsPaginated(
        organizationId: String,
        resourceHrn: ResourceHrn,
        paginationContext: PaginationContext
    ): Result<ActionsRecord> {
        return ctx("action.fetchPaginated").selectFrom(ACTIONS)
            .where(ACTIONS.ORGANIZATION_ID.eq(organizationId).and(ACTIONS.RESOURCE_HRN.eq(resourceHrn.toString())))
            .paginate(ACTIONS.HRN, paginationContext)
            .fetch()
    }

    suspend fun delete(hrn: ActionHrn): Boolean {
        val record = ActionsRecord().setHrn(hrn.toString())
        record.attach(dao().configuration())
        return record.delete() > 0
    }
}
