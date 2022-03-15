/**
 * We need this package because ResultImpl is package-private and cannot be directly used in tests.
 */
package org.jooq.impl

import io.mockk.mockk
import org.jooq.Field
import org.jooq.Record
import org.jooq.Result

fun <T : Record?> getResultImpl(records: List<T>): Result<T> {
    val result = ResultImpl<T>(mockk(), mockk<Field<T>>())
    result.addAll(records)
    return result
}
