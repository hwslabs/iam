package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.Constants.Companion.JOOQ_QUERY_NAME
import com.txman.TxMan
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.db.MetricsDSLContext
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.TableField
import org.jooq.UpdatableRecord
import org.jooq.impl.DAOImpl
import org.jooq.impl.DSL
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseRepo<R : UpdatableRecord<R>, P, T> : KoinComponent {
    protected val txMan: TxMan by inject()
    private val micrometerRegistry: MeterRegistry by inject()

    abstract suspend fun dao(): DAOImpl<R, P, T>

    suspend fun ctx(name: String? = null): DSLContext =
        MetricsDSLContext.withMetrics(dao().ctx(), micrometerRegistry, emptyList())
            .apply { name?.let { this.tag(JOOQ_QUERY_NAME, name) } }

    suspend fun delete(pojo: P) = dao().delete(pojo)

    suspend fun <Z> fetch(
        field: Field<Z>,
        vararg values: Z,
    ): List<P> = dao().fetch(field, *values)

    suspend fun <Z> fetchOne(
        field: Field<Z>,
        value: Z,
    ): P? = dao().fetchOne(field, value)

    suspend fun existsById(id: T): Boolean = dao().existsById(id)

    suspend fun insert(pojo: P) = dao().insert(pojo)

    suspend fun store(record: R): Int = record.apply { attach(dao().configuration()) }.store()

    suspend fun delete(record: R): Int = record.apply { attach(dao().configuration()) }.delete()

    suspend fun deleteBatch(keys: Collection<T>): Int {
        val pk = pk()
        requireNotNull(pk) { "Cannot batchDelete on ${dao().table.name} as primaryKey is unavailable" }
        return ctx().deleteFrom(dao().table).where(equal(pk, keys)).execute()
    }

    suspend fun insertBatch(
        queryName: String? = null,
        records: List<R>,
    ): List<R> {
        if (records.isEmpty()) return records
        var insertStep = ctx(queryName).insertInto(dao().table).set(records[0])
        for (i in 1 until records.size) {
            insertStep = insertStep.newRecord().set(records[i])
        }
        return insertStep.returning().fetch()
    }

    suspend fun batchStore(
        queryName: String? = null,
        records: List<R>,
    ) = ctx(queryName).batchStore(records).execute()

    // ------------------------------------------------------------------------
    // XXX: Private utility methods: Repurposed from JOOQ's DAOImpl.java
    // ------------------------------------------------------------------------

    private fun equal(
        pk: Array<out Field<*>>,
        id: T,
    ): Condition {
        @Suppress("SpreadOperator")
        return if (pk.size == 1) {
            (pk[0] as Field<Any>).equal(pk[0].dataType.convert(id))
        } else {
            DSL.row(*pk).equal(id as Record)
        }
    }

    private fun equal(
        pk: Array<out TableField<R, *>>,
        ids: Collection<T>,
    ): Condition {
        return if (pk.size == 1) {
            if (ids.size == 1) {
                equal(pk, ids.iterator().next())
            } else {
                (pk[0]).`in`(pk[0].dataType.convert(ids))
            }
        } else {
            // [#2573] Composite key T types are of type Record[N]
            TODO("Composite keys are unsupported at the moment")
//            DSL.row(*pk).`in`(ids as MutableCollection<RowN>)
        }
    }

    private suspend fun pk(): Array<out TableField<R, *>>? {
        return dao().table.primaryKey?.fieldsArray
    }
}
