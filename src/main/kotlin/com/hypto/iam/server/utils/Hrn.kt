package com.hypto.iam.server.utils

object Hrn {
    fun of(organization: String, resourceType: String, resourceInstance: String): String {
        return "hws.$organization.$resourceType.$resourceInstance"
    }
}
