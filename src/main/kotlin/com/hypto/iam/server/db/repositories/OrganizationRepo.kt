package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Organizations.ORGANIZATIONS
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.db.tables.records.OrganizationsRecord
import java.time.LocalDateTime
import org.jooq.impl.DAOImpl

object OrganizationRepo : BaseRepo<OrganizationsRecord, Organizations, String>() {

    private val idFun = fun (organization: Organizations): String {
        return organization.id
    }

    override suspend fun dao(): DAOImpl<OrganizationsRecord, Organizations, String> =
        txMan.getDao(ORGANIZATIONS, Organizations::class.java, idFun)

    /**
     * Updates organization with given input values
     */
    suspend fun update(id: String, name: String?, description: String?): OrganizationsRecord? {
        val updateStep = ctx("organizations.update")
            .update(ORGANIZATIONS)
            .set(ORGANIZATIONS.UPDATED_AT, LocalDateTime.now())
        if (!name.isNullOrEmpty()) {
            updateStep.set(ORGANIZATIONS.NAME, name)
        }
        if (!description.isNullOrEmpty()) {
            updateStep.set(ORGANIZATIONS.DESCRIPTION, description)
        }
        return updateStep.where(ORGANIZATIONS.ID.eq(id)).returning().fetchOne()
    }

    suspend fun findById(id: String): Organizations? = dao().findById(id)

    suspend fun deleteById(id: String) = dao().deleteById(id)

    suspend fun create(organization: Organizations) {
        dao()
            .ctx()
            .insertInto(ORGANIZATIONS)
            .values(organization)
            .returning(ORGANIZATIONS.ID)
            .fetch()
    }
}
