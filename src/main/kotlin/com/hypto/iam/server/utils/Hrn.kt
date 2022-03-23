@file:Suppress("MaximumLineLength")

package com.hypto.iam.server.utils

import com.hypto.iam.server.utils.ActionHrn.Companion.ACTION_HRN_REGEX
import com.hypto.iam.server.utils.ResourceHrn.Companion.RESOURCE_HRN_REGEX
import io.ktor.request.ApplicationRequest
import io.ktor.request.path

/**
 * Hrn - Hypto Resource Name. It is a unique representation of an entity managed by iam service.
 * Hrn entities are classified into 1) ResourceHrn 2) GlobalHrn
 * 1. ResourceHrn - This identifies the instances of resources like users, policies, and any other service specific resources.
 * 2. ActionHrn - This identifies the resource names and operations possible with the resources.
 */
abstract class Hrn {
    companion object {
        const val HRN_DELIMITER = ":"
        const val HRN_PREFIX = "hrn$HRN_DELIMITER"
        const val HRN_ACTION_DELIMITER = "$"
        const val HRN_INSTANCE_DELIMITER = "/"

        // TODO: This can be clubbed with the IamResources object
        val resourceMap: Map<String, String> = mapOf(
            "users" to "iam-user",
            "resources" to "iam-resource",
            "policies" to "iam-policy",
            "actions" to "iam-action",
            "credentials" to "iam-credential"
        )
    }

    abstract val organization: String
    abstract val resource: String?
}

/**
 * Class representing ResourceHrn
 */
class ResourceHrn : Hrn {
    override val organization: String
    override val resource: String?
    val account: String?
    val resourceInstance: String?

    companion object {
        val RESOURCE_HRN_REGEX =
            """^hrn:(?<organization>[^:\n]+):(?<accountId>[^:\n]*):(?<resource>[^:/\n]*)/{0,1}(?<resourceInstance>[^/\n:]*)""".toRegex()
        private const val URL_SEPARATOR = '/'
    }

    constructor(
        organization: String,
        account: String? = null,
        resource: String,
        resourceInstance: String?
    ) {
        this.organization = organization
        this.account = account
        if (resource == "") {
            // Resource Hrn must have a resource
            throw HrnParseException("Null resource for resource hrn")
        }
        this.resource = resource
        this.resourceInstance = resourceInstance
    }

    constructor(hrnString: String) {
        val result = RESOURCE_HRN_REGEX.matchEntire(hrnString)
            ?: throw HrnParseException("Not a valid hrn string format")
        organization = result.groups["organization"]!!.value
        account = result.groups["accountId"]?.value
        resource = result.groups["resource"]?.value
        resourceInstance = result.groups["resourceInstance"]?.value
    }

    constructor(request: ApplicationRequest) {
        // Valid application request paths
        // 1. /organization/<organizationId>/
        // 2. /organization/<organizationId>/user/ - hrn:hypto:<accountId>:iam-organization - createUser
        // 3. /organization/<organizationId>/resource/ - hrn:hypto:<accountId>:iam-organization - createResource
        // 4. /organization/<organizationId>/user/<userId> - hrn:hypto:<accountId>:iam-user/12345 - updateUser
        // 5. /organization/<organizationId>/resource/<resourceId> - hrn:hypto:<accountId>:iam-resource/12345 - updateResource
        // 6. /organization/<organizationId>/user/<userId>/credentials/ - hrn:hypto:<accountId>:iam-user/12345 - addCredentials
        // 7. /organization/<organizationId>/resource/<resourceId>/action/ - hrn:hypto:<accountId>:iam-resource/12345 - addAction
        // 8. /organization/<organizationId>/user/<userId>/credentials/<credentialsId> - hrn:hypto:<accountId>:iam-credential/12345 - getCredentials
        // 9. /organization/<organizationId>/resource/<resourceId>/action/<actionId> - hrn:hypto:<accountId>:iam-action/12345 - updateAction

        val splits = request.path().trim(URL_SEPARATOR).split(URL_SEPARATOR)
        if (splits.size < 2) throw IllegalArgumentException("Invalid request path")
        this.organization = splits[1]
        this.account = null
        val resourceAndInstance = getResourceAndInstance(splits)
        this.resource = resourceAndInstance.first
        this.resourceInstance = resourceAndInstance.second
    }

    private fun getResourceAndInstance(splits: List<String>): Pair<String, String> {
        if (splits.size == 2) return Pair("", "")
        val lastSplitMap = resourceMap[splits.last()]
        return if (lastSplitMap.isNullOrEmpty()) {
            val resource = resourceMap[splits[(splits.lastIndex - 1)]] ?: ""
            val resourceInstance = splits.last()
            Pair(resource, resourceInstance)
        } else {
            val resourceInstance = splits[(splits.lastIndex - 1)] ?: ""
            val resource = resourceMap[splits[(splits.lastIndex - 2)]] ?: ""
            Pair(resource, resourceInstance)
        }
    }

    override fun toString(): String {
        // Valid Hrn Strings
        // 1. hrn:<organizationId>
        // 2. hrn:<organizationId>:<accountId>
        // 3. hrn:<organizationId>::<resourceType>/<resourceInstance>
        // 4. hrn:<organizationId>:<accountId>:<resourceType>/<resourceInstance>

        var hrnString = HRN_PREFIX + organization
        if (account.isNullOrEmpty() && resource.isNullOrEmpty()) {
            return hrnString
        }
        hrnString += HRN_DELIMITER + (account ?: "")
        if (!resource.isNullOrEmpty()) {
            hrnString += HRN_DELIMITER + resource
            if (!resourceInstance.isNullOrEmpty()) {
                hrnString += HRN_INSTANCE_DELIMITER + resourceInstance
            }
        }
        return hrnString
    }
}

/**
 * Class representing GlobalHrn
 */
class ActionHrn : Hrn {
    override val organization: String
    val account: String?
    override val resource: String?
    val action: String?

    companion object {
        val ACTION_HRN_REGEX =
            """^hrn:(?<organization>[^:\n]+):(?<accountId>[^:\n]*):(?<resource>[^:/\n]*)\$(?<action>[^/\n:]*)"""
                .toRegex()
    }

    constructor(organization: String, account: String? = null, resource: String?, action: String?) {
        this.organization = organization
        this.account = account
        this.resource = resource
        this.action = action
    }

    constructor(hrnString: String) {
        val result = ACTION_HRN_REGEX.matchEntire(hrnString)
            ?: throw HrnParseException("Not a valid hrn action string format")
        organization = result.groups["organization"]!!.value
        resource = result.groups["resource"]?.value
        account = result.groups["accountId"]?.value
        action = result.groups["action"]?.value
    }

    override fun toString(): String {
        var hrnString = HRN_PREFIX + organization + HRN_DELIMITER
        if (!account.isNullOrEmpty())
            hrnString += account
        hrnString += HRN_DELIMITER + resource
        if (!action.isNullOrEmpty()) {
            hrnString += HRN_ACTION_DELIMITER + action
        }
        return hrnString
    }
}

object HrnFactory {
    /**
     * Method to convert the Hrn string to actual Hrn instance
     */
    fun getHrn(hrnString: String): Hrn {
        return if (ACTION_HRN_REGEX.matches(hrnString)) {
            ActionHrn(hrnString)
        } else if (RESOURCE_HRN_REGEX.matches(hrnString)) {
            ResourceHrn(hrnString)
        } else {
            throw HrnParseException("Invalid hrn string format")
        }
    }

    fun isValid(hrnString: String): Boolean {
        return try {
            getHrn(hrnString)
            true
        } catch (e: HrnParseException) {
            false
        }
    }
}

class HrnParseException(override val message: String) : Exception(message)
