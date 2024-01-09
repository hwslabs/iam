@file:Suppress("MaximumLineLength")

package com.hypto.iam.server.utils

import com.hypto.iam.server.utils.ActionHrn.Companion.ACTION_HRN_REGEX
import com.hypto.iam.server.utils.ResourceHrn.Companion.RESOURCE_HRN_REGEX

/**
 * Hrn - Hypto Resource Name. It is a unique representation of an entity managed by iam service.
 * Hrn entities are classified into 1) ResourceHrn 2) GlobalHrn
 * 1. ResourceHrn - This identifies the instances of resources like users, policies, and any other service specific resources.
 * 2. ActionHrn - This identifies the resource names and operations possible with the resources.
 */
@Suppress("UnnecessaryAbstractClass") // Required as child classes are using variables: "resource" and "organization"
abstract class Hrn {
    companion object {
        const val HRN_DELIMITER = ":"
        const val HRN_PREFIX = "hrn$HRN_DELIMITER"
        const val HRN_ACTION_DELIMITER = "$"
        const val HRN_INSTANCE_DELIMITER = "/"
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
    private val subOrganization: String?
    val resourceInstance: String?

    companion object {
        val RESOURCE_HRN_REGEX =
            (
                "^hrn:(?<organization>[^:\n]+):" +
                    "(?<subOrganization>[^:\n]*):(?<resource>[^:/\n]*)/?(?<resourceInstance>[^/\n:]*)"
                ).trimMargin().toRegex()
    }

    constructor(
        organization: String,
        subOrganization: String? = null,
        resource: String,
        resourceInstance: String?
    ) {
        this.organization = organization
        this.subOrganization = subOrganization
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
        subOrganization = result.groups["subOrganization"]?.value
        resource = result.groups["resource"]?.value
        resourceInstance = result.groups["resourceInstance"]?.value
    }

    override fun toString(): String {
        // Valid Hrn Strings
        // 1. hrn:<organizationId>
        // 2. hrn:<organizationId>:<subOrganizationId>
        // 3. hrn:<organizationId>::<resourceType>/<resourceInstance>
        // 4. hrn:<organizationId>:<subOrganizationId>:<resourceType>/<resourceInstance>

        var hrnString = HRN_PREFIX + organization
        if (subOrganization.isNullOrEmpty() && resource.isNullOrEmpty()) {
            return hrnString
        }
        hrnString += HRN_DELIMITER + (subOrganization ?: "")
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
 * Class representing ActionHrn
 */
class ActionHrn : Hrn {
    override val organization: String
    val subOrganization: String?
    override val resource: String?
    val action: String?

    companion object {
        val ACTION_HRN_REGEX =
            """^hrn:(?<organization>[^:\n]+):(?<subOrganization>[^:\n]*):(?<resource>[^:/\n]*)\$(?<action>[^/\n:]*)"""
                .toRegex()
    }

    constructor(organization: String, subOrganization: String? = null, resource: String?, action: String?) {
        this.organization = organization
        this.subOrganization = subOrganization
        this.resource = resource
        this.action = action
    }

    constructor(hrnString: String) {
        val result = ACTION_HRN_REGEX.matchEntire(hrnString)
            ?: throw HrnParseException("Not a valid hrn action string format")
        organization = result.groups["organization"]!!.value
        resource = result.groups["resource"]?.value
        subOrganization = result.groups["subOrganization"]?.value
        action = result.groups["action"]?.value
    }

    override fun toString(): String {
        var hrnString = HRN_PREFIX + organization + HRN_DELIMITER
        if (!subOrganization.isNullOrEmpty()) {
            hrnString += subOrganization
        }
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
