@file:Suppress("MaximumLineLength")
package com.hypto.iam.server.utils

import com.hypto.iam.server.utils.GlobalHrn.Companion.GLOBAL_HRN_REGEX
import com.hypto.iam.server.utils.Hrn.Companion.HRN_DELIMITER
import com.hypto.iam.server.utils.Hrn.Companion.HRN_GLOBAL_DELIMITER
import com.hypto.iam.server.utils.Hrn.Companion.HRN_INSTANCE_DELIMITER
import com.hypto.iam.server.utils.Hrn.Companion.HRN_PREFIX
import com.hypto.iam.server.utils.Hrn.Companion.resourceMap
import com.hypto.iam.server.utils.ResourceHrn.Companion.RESOURCE_HRN_REGEX
import io.ktor.request.ApplicationRequest
import io.ktor.request.path

/**
 * Hrn - Hypto Resource Name. It is a unique representation of an entity managed by iam service.
 * Hrn entities are classified into 1) ResourceHrn 2) GlobalHrn
 * 1. ResourceHrn - This identifies the instances of resources like users, policies, and any other service specific resources.
 * 2. GlobalHrn - This identifies the resource names and operations possible with the resources.
 */
interface Hrn {
    companion object {
        const val HRN_DELIMITER = ":"
        const val HRN_PREFIX = "hrn$HRN_DELIMITER"
        const val HRN_GLOBAL_DELIMITER = "$"
        const val HRN_INSTANCE_DELIMITER = "/"
        val resourceMap: Map<String, String> = mapOf(
            "user" to "iam-user",
            "resource" to "iam-resource",
            "policy" to "iam-policy",
            "action" to "iam-action",
            "credential" to "iam-credential"
        )
    }
}

/**
 * Class representing ResourceHrn
 */
class ResourceHrn : Hrn {
    val organization: String
    val account: String?
    val resourceType: String?
    val resourceInstance: String?

    companion object {
        val RESOURCE_HRN_REGEX = """^hrn:(?<organization>[^:\n]+):(?<accountId>[^:\n]*):(?<resourceType>[^:/\n]*)/{0,1}(?<resourceInstance>[^/\n:]*)""".toRegex()
    }

    constructor(
        organization: String,
        account: String?,
        resourceType: String?,
        resourceInstance: String?
    ) {
        this.organization = organization
        this.account = account
        if (resourceType != null && resourceInstance == null) {
            // Resource Hrn must have a resource instance
            throw HrnParseException("Null resourceInstance for resource hrn")
        }
        this.resourceType = resourceType
        this.resourceInstance = resourceInstance
    }

    constructor(hrnString: String) {
        val result = RESOURCE_HRN_REGEX.matchEntire(hrnString)
            ?: throw HrnParseException("Not a valid hrn string format")
        organization = result.groups["organization"]!!.value
        account = result.groups["accountId"]?.value
        resourceType = result.groups["resourceType"]?.value
        resourceInstance = result.groups["resourceInstance"]?.value
    }

    constructor(request: ApplicationRequest) {
        val requestPath = request.path()
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

        val splits = requestPath.split("/")
        if (splits.size < 2) throw IllegalArgumentException("Invalid request path")
        this.organization = splits[1]
        this.account = null
        this.resourceType = getResourceTypeAndInstance(splits).first
        this.resourceInstance = getResourceTypeAndInstance(splits).second
    }

    private fun getResourceTypeAndInstance(splits: List<String>): Pair<String, String> {
        if (splits.size == 2) return Pair("", "")
        val lastSplitMap = resourceMap[splits.last()]
        return if (lastSplitMap.isNullOrEmpty()) {
            val resourceType = resourceMap[splits[(splits.lastIndex - 1)]] ?: ""
            val resourceInstance = lastSplitMap ?: ""
            Pair(resourceType, resourceInstance)
        } else {
            val resourceInstance = resourceMap[splits[(splits.lastIndex - 1)]] ?: ""
            val resourceType = resourceMap[splits[(splits.lastIndex - 2)]] ?: ""
            Pair(resourceType, resourceInstance)
        }
    }

    private fun getResourceInstance(splits: List<String>): String? {
        val resType = splits.getOrNull(5)
        return resourceMap.getOrDefault(resType, "")
    }

    private fun getResourceType(splits: List<String>): String? {
        val resType = splits.getOrNull(2)
        return resourceMap.getOrDefault(resType, "")
    }

    override fun toString(): String {
        // Valid Hrn Strings
        // 1. hrn:<organizationId>
        // 2. hrn:<organizationId>:<accountId>
        // 3. hrn:<organizationId>::<resourceType>/<resourceInstance>
        // 4. hrn:<organizationId>:<accountId>:<resourceType>/<resourceInstance>

        var hrnString = HRN_PREFIX + organization
        if (!account.isNullOrEmpty()) {
            hrnString += account
        }
        if (!resourceType.isNullOrEmpty()) {
            if (account.isNullOrEmpty()) {
                hrnString += HRN_DELIMITER + HRN_DELIMITER
            }
            hrnString += resourceType + HRN_INSTANCE_DELIMITER + resourceInstance
        }
        return hrnString
    }
}

/**
 * Class representing GlobalHrn
 */
class GlobalHrn : Hrn {
    val organization: String
    val resourceType: String?
    val operation: String?

    companion object {
        val GLOBAL_HRN_REGEX = """^hrn:(?<organization>[^:$\n]+)\$(?<resourceType>[^:\n]*):{0,1}(?<operation>[^/\n:]*)""".toRegex()
    }

    constructor(organization: String, resource: String?, operation: String?) {
        this.organization = organization
        this.resourceType = resource
        this.operation = operation
    }

    constructor(hrnString: String) {
        val result = GLOBAL_HRN_REGEX.matchEntire(hrnString) ?: throw HrnParseException("Not a valid hrn string format")
        organization = result.groups["organization"]!!.value
        resourceType = result.groups["resourceType"]?.value
        operation = result.groups["operation"]?.value
    }

    override fun toString(): String {
        var hrnString = HRN_PREFIX + organization + HRN_GLOBAL_DELIMITER + resourceType
        if (!operation.isNullOrEmpty()) {
            hrnString += HRN_DELIMITER + operation
        }
        return hrnString
    }
}

class HrnFactory {
    /**
     * Method to convert the Hrn string to actual Hrn instance
     */
    fun getHrn(hrnString: String): Hrn {
        return if (RESOURCE_HRN_REGEX.matches(hrnString)) {
            ResourceHrn(hrnString)
        } else if (GLOBAL_HRN_REGEX.matches(hrnString)) {
            GlobalHrn(hrnString)
        } else {
            throw HrnParseException("Invalid hrn string format")
        }
    }
}

class HrnParseException(override val message: String) : Exception(message)
