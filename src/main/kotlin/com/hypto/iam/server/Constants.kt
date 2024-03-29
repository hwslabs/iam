package com.hypto.iam.server

import com.hypto.iam.server.models.PaginationOptions

class Constants private constructor() {
    // Validation constants
    companion object {
        const val MIN_LENGTH = 2
        const val MIN_USERNAME_LENGTH = 8
        const val MIN_EMAIL_LENGTH = 4
        const val MAX_NAME_LENGTH = 50
        const val MAX_SUB_ORG_LENGTH = 1200
        const val MAX_USERNAME_LENGTH = 50
        const val MIN_DESC_LENGTH = 2
        const val MAX_DESC_LENGTH = 100
        const val MIN_POLICY_STATEMENTS = 1
        const val MAX_POLICY_STATEMENTS = 50
        const val MAX_POLICY_ASSOCIATIONS_PER_REQUEST = 20
        const val MINIMUM_PHONE_NUMBER_LENGTH = 8
        const val MINIMUM_PASSWORD_LENGTH = 8

        const val PAGINATION_MAX_PAGE_SIZE = 50
        const val PAGINATION_DEFAULT_PAGE_SIZE = 50
        val PAGINATION_DEFAULT_SORT_ORDER = PaginationOptions.SortOrder.asc
        const val NEWRELIC_METRICS_PUBLISH_INTERVAL = 30L // seconds
        const val X_ORGANIZATION_HEADER = "X-Iam-User-Organization"
        const val X_API_KEY_HEADER = "X-Api-Key"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val SECRET_PREFIX = "$"
        const val JOOQ_QUERY_NAME = "queryName"
        const val SECONDS_IN_DAY = 24 * 60 * 60L

        const val POLICY_NAME = "policy_name"
        const val ORGANIZATION_ID_KEY = "organization_id"
        const val SUB_ORGANIZATION_ID_KEY = "sub_organization_id"
        const val USER_HRN_KEY = "user_hrn"
        const val USER_ID = "user_id"
    }
}
