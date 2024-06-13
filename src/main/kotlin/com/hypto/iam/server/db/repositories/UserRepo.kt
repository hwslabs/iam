package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Users.USERS
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.models.User
import org.jooq.Condition
import org.jooq.impl.DAOImpl
import org.jooq.impl.DSL
import java.time.LocalDateTime

@Suppress("TooManyFunctions")
object UserRepo : BaseRepo<UsersRecord, Users, String>() {
    private val idFun = fun(user: Users) = user.hrn

    override suspend fun dao(): DAOImpl<UsersRecord, Users, String> =
        txMan.getDao(USERS, Users::class.java, idFun)

    suspend fun insert(usersRecord: UsersRecord): UsersRecord? {
        return ctx("users.insert").insertInto(USERS).set(usersRecord).returning().fetchOne()
    }

    suspend fun findByHrn(hrn: String): UsersRecord? =
        ctx("users.findByHrn")
            .selectFrom(USERS)
            .where(USERS.HRN.eq(hrn))
            .and(USERS.DELETED.eq(false))
            .fetchOne()

    suspend fun findByHrns(hrn: List<String>): Map<String, UsersRecord> =
        ctx("users.findByHrns")
            .selectFrom(USERS)
            .where(USERS.HRN.`in`(hrn))
            .and(USERS.DELETED.eq(false))
            .fetchMap(USERS.HRN)

    suspend fun fetchUsers(
        organizationId: String,
        subOrganizationName: String?,
        paginationContext: PaginationContext,
    ): List<UsersRecord> {
        return ctx("users.fetchMany").selectFrom(USERS)
            .where(USERS.ORGANIZATION_ID.eq(organizationId))
            .apply {
                subOrganizationName?.let {
                    and(USERS.SUB_ORGANIZATION_NAME.eq(it))
                } ?: and(USERS.SUB_ORGANIZATION_NAME.isNull)
            }
            .and(USERS.DELETED.eq(false))
            .paginate(USERS.CREATED_AT, paginationContext)
            .fetch()
    }

    suspend fun existsByAliasUsername(
        preferredUsername: String?,
        email: String?,
        organizationId: String,
        subOrganizationName: String?,
        uniqueCheck: Boolean = false,
    ): Boolean {
        val conditions = mutableListOf<Condition>()

        if (!email.isNullOrEmpty()) {
            conditions.add(USERS.EMAIL.eq(email))
        }
        if (!preferredUsername.isNullOrEmpty()) {
            conditions.add(USERS.PREFERRED_USERNAME.eq(preferredUsername))
        }

        var builder =
            DSL.selectFrom(USERS)
                .where(DSL.or(conditions))
                .and(USERS.DELETED.eq(false))

        builder =
            if (uniqueCheck) {
                // Check only verified users in other organizations and all users within the organization
                builder.and(
                    (
                        USERS.VERIFIED.eq(true)
                            .andNot(USERS.ORGANIZATION_ID.eq(organizationId))
                    )
                        .or(USERS.ORGANIZATION_ID.eq(organizationId)),
                )
            } else {
                // Check all verified and unverified users within the organization
                builder.and(USERS.ORGANIZATION_ID.eq(organizationId))
                    .apply { subOrganizationName?.let { and(USERS.SUB_ORGANIZATION_NAME.eq(it)) } ?: and(USERS.SUB_ORGANIZATION_NAME.isNull) }
            }

        return ctx("users.existsByAliasUsername").fetchExists(builder)
    }

    suspend fun update(
        hrn: String,
        status: User.Status? = null,
        verified: Boolean? = null,
        name: String? = null,
        loginAccess: Boolean? = null,
    ): UsersRecord? {
        val updateStep = ctx("users.update").update(USERS).set(USERS.UPDATED_AT, LocalDateTime.now())
        status?.let { updateStep.set(USERS.STATUS, it.value) }
        verified?.let { updateStep.set(USERS.VERIFIED, it) }
        name?.let { updateStep.set(USERS.NAME, it) }
        loginAccess?.let { updateStep.set(USERS.LOGIN_ACCESS, it) }
        return updateStep.where(USERS.HRN.eq(hrn)).and(USERS.DELETED.eq(false))
            .returning().fetchOne()
    }

    suspend fun delete(hrn: String): UsersRecord? =
        ctx("users.delete")
            .update(USERS)
            .set(USERS.UPDATED_AT, LocalDateTime.now())
            .set(USERS.DELETED, true)
            .where(USERS.HRN.eq(hrn))
            .returning().fetchOne()

    suspend fun findByEmail(
        email: String,
        organizationId: String? = null,
        subOrganizationName: String? = null,
    ): UsersRecord? {
        val builder =
            ctx("users.findByEmail")
                .selectFrom(USERS)
                .where(USERS.EMAIL.equalIgnoreCase(email))
                .and(USERS.DELETED.eq(false))
                .and(USERS.VERIFIED.eq(true))
                .apply {
                    subOrganizationName?.let {
                        and(USERS.SUB_ORGANIZATION_NAME.eq(it))
                    } ?: and(USERS.SUB_ORGANIZATION_NAME.isNull)
                }
                .apply { organizationId?.let { and(USERS.ORGANIZATION_ID.eq(it)) } }
        return builder.fetchOne()
    }

    suspend fun findSubOrgByEmail(
        email: String,
        organizationId: String,
    ): UsersRecord? {
        return ctx("users.findSubOrgByEmail")
            .selectFrom(USERS)
            .where(USERS.EMAIL.equalIgnoreCase(email))
            .and(USERS.DELETED.eq(false))
            .and(USERS.VERIFIED.eq(true))
            .and(USERS.ORGANIZATION_ID.eq(organizationId))
            .and(USERS.SUB_ORGANIZATION_NAME.isNotNull)
            .fetchOne()
    }

    suspend fun findByPreferredUsername(preferredUsername: String): UsersRecord? {
        return ctx("users.findByAliasUsername")
            .selectFrom(USERS)
            .where(USERS.PREFERRED_USERNAME.eq(preferredUsername))
            .and(USERS.DELETED.eq(false))
            .and(USERS.VERIFIED.eq(true))
            .fetchOne()
    }

    suspend fun deleteByOrganizationId(organizationId: String): Boolean {
        val count =
            ctx("users.delete_by_organization_id")
                .deleteFrom(USERS)
                .where(USERS.ORGANIZATION_ID.eq(organizationId))
                .execute()
        return count > 0
    }
}
