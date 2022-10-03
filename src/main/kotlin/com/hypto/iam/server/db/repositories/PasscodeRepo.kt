package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.PASSCODES
import com.hypto.iam.server.db.tables.pojos.Passcodes
import com.hypto.iam.server.db.tables.records.PasscodesRecord
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

    suspend fun getValidPasscodeCount(email: String, purpose: VerifyEmailRequest.Purpose): Int =
        ctx("passcodes.getValidCount")
            .selectCount()
            .from(PASSCODES)
            .where(PASSCODES.EMAIL.eq(email)).and(
                PASSCODES.PURPOSE.eq(purpose.toString()).and(
                    PASSCODES.VALID_UNTIL.ge(
                        LocalDateTime.now()
                    )
                )
            ).fetchOne(0, Int::class.java) ?: 0

    suspend fun getValidPasscode(
        id: String,
        purpose: VerifyEmailRequest.Purpose,
        email: String? = null
    ): PasscodesRecord? {
        return ctx("passcodes.getValid")
            .selectFrom(PASSCODES)
            .where(
                PASSCODES.ID.eq(id),
                PASSCODES.PURPOSE.eq(purpose.toString()),
                PASSCODES.VALID_UNTIL.ge(LocalDateTime.now())
            ).apply {
                email?.let {
                    and(PASSCODES.EMAIL.eq(email))
                }
            }
            .fetchOne()
    }

    suspend fun deleteByEmailAndPurpose(email: String, purpose: VerifyEmailRequest.Purpose): Boolean {
        val count = ctx("passcodes.deleteByEmailAndPurpose")
            .deleteFrom(PASSCODES)
            .where(
                PASSCODES.EMAIL.eq(email),
                PASSCODES.PURPOSE.eq(purpose.toString())
            )
            .execute()
        return count > 0
    }
}
