package com.hypto.iam.server

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
    }
}
