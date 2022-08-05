package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.Constants.Companion.JOOQ_QUERY_NAME
import com.txman.TxMan
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.db.MetricsDSLContext
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.UpdatableRecord
import org.jooq.impl.DAOImpl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseRepo<R : UpdatableRecord<R>?, P, T> : KoinComponent {

    protected val txMan: TxMan by inject()
    private val micrometerRegistry: MeterRegistry by inject()

    abstract suspend fun dao(): DAOImpl<R, P, T>

    suspend fun ctx(name: String? = null): DSLContext =
        MetricsDSLContext.withMetrics(dao().ctx(), micrometerRegistry, emptyList())
            .apply { name?.let { this.tag(JOOQ_QUERY_NAME, name) } }

    suspend fun delete(pojo: P) = dao().delete(pojo)

    suspend fun <Z> fetch(field: Field<Z>, vararg values: Z): List<P> = dao().fetch(field, *values)

    suspend fun <Z> fetchOne(field: Field<Z>, value: Z): P? = dao().fetchOne(field, value)

    suspend fun existsById(id: T): Boolean = dao().existsById(id)
}
