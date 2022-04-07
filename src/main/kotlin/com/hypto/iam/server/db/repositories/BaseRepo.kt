package com.hypto.iam.server.db.repositories

import com.txman.TxMan
import org.jooq.Field
import org.jooq.UpdatableRecord
import org.jooq.impl.DAOImpl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseRepo<R : UpdatableRecord<R>?, P, T> : KoinComponent {

    protected val txMan: TxMan by inject()

    abstract suspend fun dao(): DAOImpl<R, P, T>

    suspend fun delete(pojo: P) {
        dao().delete(pojo)
    }

    suspend fun <Z> fetch(field: Field<Z>, vararg values: Z): List<P> {
        return dao().fetch(field, *values)
    }

    suspend fun <Z> fetchOne(field: Field<Z>, value: Z): P? {
        return dao().fetchOne(field, value)
    }

    suspend fun existsById(id: T): Boolean {
        return dao().existsById(id)
    }
}
