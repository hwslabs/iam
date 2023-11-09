package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.PASSCODES
import com.hypto.iam.server.db.tables.pojos.Passcodes
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.models.VerifyEmailRequest
import java.time.LocalDateTime
import org.jooq.impl.DAOImpl

object PasscodeRepo : BaseRepo<PasscodesRecord, Passcodes, String>() {

    private val idFun = fun(passcodes: Passcodes) = passcodes.id

    override suspend fun dao(): DAOImpl<PasscodesRecord, Passcodes, String> {
        return txMan.getDao(com.hypto.iam.server.db.tables.Passcodes.PASSCODES, Passcodes::class.java, idFun)
    }

    suspend fun createPasscode(
        record: PasscodesRecord
    ): PasscodesRecord {
        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun getValidPasscodeCount(
        email: String,
        purpose: VerifyEmailRequest.Purpose,
        organizationId: String? = null
    ): Int =
        ctx("passcodes.getValidCount")
            .selectCount()
            .from(PASSCODES)
            .where(PASSCODES.EMAIL.eq(email)).and(
                PASSCODES.PURPOSE.eq(purpose.toString()).and(
                    PASSCODES.VALID_UNTIL.ge(
                        LocalDateTime.now()
                    )
                )
            )
            .apply {
                organizationId?.let {
                    and(PASSCODES.ORGANIZATION_ID.eq(organizationId))
                }
            }
            .fetchOne(0, Int::class.java) ?: 0

    suspend fun getValidPasscodeById(
        id: String,
        purpose: VerifyEmailRequest.Purpose,
        email: String? = null,
        organizationId: String? = null
    ): PasscodesRecord? {
        return ctx("passcodes.getValidPasscodeById")
            .selectFrom(PASSCODES)
            .where(
                PASSCODES.ID.eq(id),
                PASSCODES.PURPOSE.eq(purpose.toString()),
                PASSCODES.VALID_UNTIL.ge(LocalDateTime.now())
            ).apply {
                email?.let {
                    and(PASSCODES.EMAIL.eq(email))
                }
                organizationId?.let {
                    and(PASSCODES.ORGANIZATION_ID.eq(organizationId))
                }
            }
            .fetchOne()
    }

    suspend fun getValidPasscodeByEmail(
        organizationId: String,
        purpose: VerifyEmailRequest.Purpose,
        email: String
    ): PasscodesRecord? {
        return ctx("passcodes.getValidPasscodeByEmail")
            .selectFrom(PASSCODES)
            .where(
                PASSCODES.EMAIL.eq(email),
                PASSCODES.PURPOSE.eq(purpose.toString()),
                PASSCODES.VALID_UNTIL.ge(LocalDateTime.now()),
                PASSCODES.ORGANIZATION_ID.eq(organizationId)
            )
            .fetchOne()
    }

    suspend fun listPasscodes(
        organizationId: String,
        purpose: VerifyEmailRequest.Purpose? = null,
        paginationContext: PaginationContext
    ): List<PasscodesRecord> {
        return ctx("passcodes.list")
            .selectFrom(PASSCODES)
            .where(PASSCODES.ORGANIZATION_ID.eq(organizationId))
            .apply {
                purpose?.let {
                    and(PASSCODES.PURPOSE.eq(purpose.toString()))
                }
            }
            .and(PASSCODES.VALID_UNTIL.ge(LocalDateTime.now()))
            .paginate(PASSCODES.CREATED_AT, paginationContext)
            .fetch()
    }

    suspend fun deleteByEmailAndPurpose(
        email: String,
        purpose: VerifyEmailRequest.Purpose,
        organizationId: String? = null
    ): Boolean {
        val count = ctx("passcodes.deleteByEmailAndPurpose")
            .deleteFrom(PASSCODES)
            .where(
                PASSCODES.EMAIL.eq(email),
                PASSCODES.PURPOSE.eq(purpose.toString())
            )
            .apply {
                organizationId?.let {
                    and(PASSCODES.ORGANIZATION_ID.eq(organizationId))
                }
            }
            .execute()
        return count > 0
    }

    suspend fun deleteByOrganizationId(organizationId: String): Boolean {
        val count = ctx("passcodes.delete_by_organization_id")
            .deleteFrom(PASSCODES)
            .where(PASSCODES.ORGANIZATION_ID.eq(organizationId))
            .execute()
        return count > 0
    }
}
