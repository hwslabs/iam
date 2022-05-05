package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.PASSCODES
import com.hypto.iam.server.db.tables.pojos.Passcodes
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import java.time.LocalDateTime
import java.util.UUID
import org.jooq.impl.DAOImpl

object PasscodeRepo : BaseRepo<PasscodesRecord, Passcodes, UUID>() {

    private val idFun = fun(passcodes: Passcodes): UUID {
        return passcodes.id
    }

    override suspend fun dao(): DAOImpl<PasscodesRecord, Passcodes, UUID> {
        return txMan.getDao(com.hypto.iam.server.db.tables.Passcodes.PASSCODES, Passcodes::class.java, idFun)
    }

    suspend fun createPasscode(
        email: String,
        organizationId: String?,
        validUntil: LocalDateTime,
        type: Type
    ): PasscodesRecord {
        val record = PasscodesRecord().setValidUntil(validUntil).setType(type.toString()).setEmail(email)
            .setOrganizationId(organizationId).setCreatedAt(LocalDateTime.now())
        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun getValidPasscode(id: UUID, type: Type, email: String): PasscodesRecord? {
        val dao = dao()
        return dao().ctx().selectFrom(dao.table)
            .where(
                PASSCODES.ID.eq(id).and(PASSCODES.TYPE.eq(type.toString()))
                    .and(PASSCODES.VALID_UNTIL.ge(LocalDateTime.now())).and(PASSCODES.EMAIL.eq(email))
            ).fetchOne()
    }

    suspend fun deleteById(id: UUID): Boolean {
        val record = PasscodesRecord().setId(id)
        record.attach(dao().configuration())
        val count = record.delete()
        return count > 0
    }

    enum class Type(val value: String) {
        RESET("RESET"),
        VERIFY("VERIFY")
    }
}
