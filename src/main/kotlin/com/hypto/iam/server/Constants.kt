package com.hypto.iam.server

import com.hypto.iam.server.models.PaginationOptions

class Constants private constructor() {
    // Validation constants
    companion object {
        const val MIN_NAME_LENGTH = 2
        const val MAX_NAME_LENGTH = 50
        const val MIN_DESC_LENGTH = 2
        const val MAX_DESC_LENGTH = 100
        const val MIN_POLICY_STATEMENTS = 1
        const val MAX_POLICY_STATEMENTS = 50
        const val MAX_POLICY_ASSOCIATIONS_PER_REQUEST = 20

        const val PAGINATION_MAX_PAGE_SIZE = 50
        const val PAGINATION_DEFAULT_PAGE_SIZE = 50
        val PAGINATION_DEFAULT_SORT_ORDER = PaginationOptions.SortOrder.asc
        const val NEWRELIC_METRICS_PUBLISH_INTERVAL = 30L // seconds
    }
}
