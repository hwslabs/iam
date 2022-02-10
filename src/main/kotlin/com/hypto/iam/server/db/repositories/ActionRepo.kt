package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.Actions
import com.hypto.iam.server.db.tables.records.ActionsRecord
import org.jooq.Record2
import org.jooq.impl.DAOImpl

object ActionRepo : DAOImpl<ActionsRecord, Actions, Record2<String, String>>(
    com.hypto.iam.server.db.tables.Actions.ACTIONS,
    Actions::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(action: Actions): Record2<String, String> {
        return compositeKeyRecord(action.organizationId, action.resourceType, action.name)
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(orgId: String): List<Actions> {
        return fetch(com.hypto.iam.server.db.tables.Actions.ACTIONS.ORGANIZATION_ID, orgId)
    }

    /**
     * Fetch records that have `organization_id = value AND resource_type = value`
     */
    fun fetchByOrganizationIdAndResourceType(orgId: String, resourceName: String): List<Actions> {
        return ctx()
            .selectFrom(table)
            .where(
                com.hypto.iam.server.db.tables.Actions.ACTIONS.ORGANIZATION_ID.equal(orgId),
                com.hypto.iam.server.db.tables.Actions.ACTIONS.RESOURCE_TYPE.equal(resourceName)
            )
            .fetch(mapper())
    }
}
