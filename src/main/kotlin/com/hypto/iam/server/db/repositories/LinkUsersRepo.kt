package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.AUTH_PROVIDER
import com.hypto.iam.server.db.tables.LinkUsers.LINK_USERS
import com.hypto.iam.server.db.tables.pojos.LinkUsers
import com.hypto.iam.server.db.tables.records.LinkUsersRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import org.jooq.impl.DAOImpl

object LinkUsersRepo : BaseRepo<LinkUsersRecord, LinkUsers, String>() {
    private val idFun = fun(linkUsers: LinkUsers) = linkUsers.id

    override suspend fun dao(): DAOImpl<LinkUsersRecord, LinkUsers, String> =
        txMan.getDao(LINK_USERS, LinkUsers::class.java, idFun)

    suspend fun getById(id: String) =
        ctx("linkUsers.getById").selectFrom(LINK_USERS)
            .where(LINK_USERS.ID.eq(id))
            .fetchOne()

    suspend fun fetchSubordinateUsers(
        organizationId: String,
        masterUserHrn: String,
        context: PaginationContext,
    ): Map<String, LinkUsersRecord> {
        return ctx("linkUsers.fetchSubordinateUsers")
            .selectFrom(LINK_USERS)
            .where(
                LINK_USERS.ORGANIZATION_ID.eq(organizationId),
                LINK_USERS.MASTER_USER.eq(masterUserHrn),
            )
            .paginate(LINK_USERS.SUBORDINATE_USER, context)
            .fetchMap(LINK_USERS.SUBORDINATE_USER)
    }

    suspend fun fetchMasterUsers(
        organizationId: String,
        subordinateUserHrn: String,
        context: PaginationContext,
    ): Map<String, LinkUsersRecord> {
        return ctx("linkUsers.fetchMasterUsers")
            .selectFrom(LINK_USERS)
            .where(
                LINK_USERS.ORGANIZATION_ID.eq(organizationId),
                LINK_USERS.SUBORDINATE_USER.eq(subordinateUserHrn),
            )
            .paginate(LINK_USERS.MASTER_USER, context)
            .fetchMap(LINK_USERS.MASTER_USER)
    }

    suspend fun deleteById(id: String): Boolean {
        return ctx("linkUsers.deleteById").deleteFrom(LINK_USERS)
            .where(LINK_USERS.ID.eq(id))
            .execute() > 0
    }
}
