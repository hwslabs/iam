package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.db.tables.records.OrganizationsRecord
import java.util.Optional
import org.jooq.impl.DAOImpl

object OrganizationRepo : DAOImpl<OrganizationsRecord, Organizations, String?>(
    com.hypto.iam.server.db.tables.Organizations.ORGANIZATIONS,
    Organizations::class.java, com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {

    override fun getId(organization: Organizations): String {
        return organization.id
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOneById(value: String): Organizations? {
        return fetchOne(com.hypto.iam.server.db.tables.Organizations.ORGANIZATIONS.ID, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOptionalById(value: String): Optional<Organizations?> {
        return fetchOptional(com.hypto.iam.server.db.tables.Organizations.ORGANIZATIONS.ID, value)
    }

    /**
     * Fetch records that have `name IN (values)`
     */
    fun fetchByName(vararg values: String): List<Organizations?> {
        return fetch(com.hypto.iam.server.db.tables.Organizations.ORGANIZATIONS.NAME, *values)
    }

    /**
     * Fetch records that have `admin_user IN (values)`
     */
    fun fetchByAdminUser(vararg values: String): List<Organizations?> {
        return fetch(com.hypto.iam.server.db.tables.Organizations.ORGANIZATIONS.ADMIN_USER, *values)
    }
}
