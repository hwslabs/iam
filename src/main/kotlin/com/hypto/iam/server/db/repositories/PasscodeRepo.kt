package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.PASSCODES
import com.hypto.iam.server.db.tables.pojos.Passcodes
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.models.VerifyEmailRequest
import java.time.LocalDateTime
import org.jooq.impl.DAOImpl

object PasscodeRepo : BaseRepo<PasscodesRecord, Passcodes, String>() {

    private val idFun = fun(passcodes: Passcodes): String {
        return passcodes.id
    }

    override suspend fun dao(): DAOImpl<PasscodesRecord, Passcodes, String> {
        return txMan.getDao(com.hypto.iam.server.db.tables.Passcodes.PASSCODES, Passcodes::class.java, idFun)
    }

    suspend fun createPasscode(
        id: String,
        email: String,
        organizationId: String?,
        validUntil: LocalDateTime,
        purpose: VerifyEmailRequest.Purpose
    ): PasscodesRecord {
        val record =
            PasscodesRecord().setId(id).setValidUntil(validUntil).setPurpose(purpose.toString()).setEmail(email)
                .setOrganizationId(organizationId).setCreatedAt(LocalDateTime.now())
        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun getValidPasscode(id: String, purpose: VerifyEmailRequest.Purpose, email: String): PasscodesRecord? {
        val dao = dao()
        return dao().ctx().selectFrom(dao.table)
            .where(
                PASSCODES.ID.eq(id).and(PASSCODES.PURPOSE.eq(purpose.toString()))
                    .and(PASSCODES.VALID_UNTIL.ge(LocalDateTime.now())).and(PASSCODES.EMAIL.eq(email))
            ).fetchOne()
    }

    suspend fun deleteById(id: String): Boolean {
        val record = PasscodesRecord().setId(id)
        record.attach(dao().configuration())
        val count = record.delete()
        return count > 0
    }
}
