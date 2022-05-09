package com.hypto.iam.server.utils

import com.hypto.iam.server.utils.IdGenerator.Charset
import com.hypto.iam.server.validators.nameCheck
import io.konform.validation.Valid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ApplicationIdUtil : KoinComponent {
    private const val ORGANIZATION_ID_LENGTH = 10L
    private const val REFRESH_TOKEN_RANDOM_LENGTH = 30L
    private const val REQUEST_ID_LENGTH = 15L
    private const val PASSCODE_ID_LENGTH = 10L

    object Generator {
        private val idGenerator: IdGenerator by inject()

        fun organizationId(): String {
            return idGenerator.randomId(ORGANIZATION_ID_LENGTH, Charset.ALPHANUMERIC)
        }

        // First 10 chars: alphabets (upper) representing organizationId
        // next 20 chars: alphanumeric with upper and lower case - random
        fun refreshToken(organizationId: String): String {
            return organizationId + idGenerator.timeBasedRandomId(REFRESH_TOKEN_RANDOM_LENGTH, Charset.ALPHABETS)
        }

        fun requestId(): String {
            return idGenerator.timeBasedRandomId(REQUEST_ID_LENGTH, Charset.ALPHANUMERIC)
        }

        fun passcodeId(): String {
            return idGenerator.timeBasedRandomId(PASSCODE_ID_LENGTH, Charset.ALPHANUMERIC)
        }
    }

    object Validator {
        fun organizationId(orgId: String): Boolean {
            return (orgId.length == ORGANIZATION_ID_LENGTH.toInt() && orgId.all { it.isUpperCase() })
        }

        fun name(name: String): Boolean {
            return nameCheck(name) is Valid
        }
    }
}
