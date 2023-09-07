package com.hypto.iam.server.extensions

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.hypto.iam.server.Constants.Companion.PAGINATION_DEFAULT_PAGE_SIZE
import com.hypto.iam.server.Constants.Companion.PAGINATION_DEFAULT_SORT_ORDER
import com.hypto.iam.server.Constants.Companion.PAGINATION_MAX_PAGE_SIZE
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.PaginationOptions.SortOrder
import java.util.Base64
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.SelectForUpdateStep
import org.jooq.SortField
import org.jooq.TableField

fun <R : Record, T : Any> customPaginate(
    step: SelectConditionStep<R>,
    sortField: TableField<R, T>,
    context: PaginationContext
): SelectForUpdateStep<R> {
    val selectSeekStep = step.orderBy(getSort(sortField, context))
    if (context.lastItemId == null) {
        return selectSeekStep.limit(context.pageSize)
    }
    return selectSeekStep.seek(context.lastItemId as T).limit(context.pageSize)
}

fun <R : Record, T : Any> SelectConditionStep<R>.paginate(
    sortField: TableField<R, T>,
    context: PaginationContext
): SelectForUpdateStep<R> {
    return customPaginate(this, sortField, context)
}

fun <R : Record, T> getSort(field: TableField<R, T>, context: PaginationContext): SortField<T> {
    return if (context.sortOrder == SortOrder.asc) {
        field.asc()
    } else {
        field.desc()
    }
}

class PaginationContext(val lastItemId: String?, val pageSize: Int, val sortOrder: SortOrder = SortOrder.asc) {
    companion object {
        val gson: Gson = getKoinInstance()

        /**
         * nextToken= Base64Encoded({"lastItemId": "string", "pageSize": 123, "sortOrder": "asc"})
         */

        @Suppress("SwallowedException")
        fun from(nextToken: String): PaginationContext {
            val jsonString = String(Base64.getMimeDecoder().decode(nextToken))
            val jsonObject = gson.fromJson(jsonString, JsonElement::class.java).asJsonObject

            return PaginationContext(
                jsonObject.get(PaginationContext::lastItemId.name).asString,
                jsonObject.get(PaginationContext::pageSize.name).asInt,
                SortOrder.valueOf(jsonObject.get(PaginationContext::sortOrder.name).asString)
            )
        }

        fun from(nextToken: String?, pageSize: Int?, sortOrder: SortOrder?): PaginationContext {
            require(pageSize == null || pageSize <= PAGINATION_MAX_PAGE_SIZE) {
                "Page Size must be less than or equal to $PAGINATION_MAX_PAGE_SIZE"
            }

            if (nextToken?.isNotBlank() == true) {
                return from(nextToken)
            }
            return PaginationContext(
                lastItemId = null,
                pageSize = pageSize ?: PAGINATION_DEFAULT_PAGE_SIZE,
                sortOrder = sortOrder ?: PAGINATION_DEFAULT_SORT_ORDER
            )
        }

        fun from(lastItemId: String?, oldContext: PaginationContext): PaginationContext {
            return PaginationContext(lastItemId, oldContext.pageSize, oldContext.sortOrder)
        }
    }

    val nextToken: String?
        get() {
            if (lastItemId == null) {
                return null
            }
            return String(Base64.getEncoder().encode(gson.toJson(this).encodeToByteArray()))
        }

    fun toOptions(): PaginationOptions {
        return PaginationOptions(pageSize, sortOrder)
    }
}
