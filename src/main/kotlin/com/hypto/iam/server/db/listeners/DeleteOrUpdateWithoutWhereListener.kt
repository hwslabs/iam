package com.hypto.iam.server.db.listeners

import org.jooq.ExecuteContext
import org.jooq.impl.DefaultExecuteListener

// Repurposed from: https://www.jooq.org/doc/latest/manual/sql-execution/execute-listeners/#NA7281

/**
 * Throws DeleteOrUpdateWithoutWhereException if UPDATE or DELETE statements does not contain a WHERE clause.
 *
 * @see DeleteOrUpdateWithoutWhereException
 */
class DeleteOrUpdateWithoutWhereListener : DefaultExecuteListener() {
    override fun renderEnd(ctx: ExecuteContext) {
        if (ctx.sql()!!.matches(Regex("^(?i:(UPDATE|DELETE)(?!.* WHERE ).*)$"))) {
            throw DeleteOrUpdateWithoutWhereException()
        }
    }
}

class DeleteOrUpdateWithoutWhereException : RuntimeException()
