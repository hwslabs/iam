package com.hypto.iam.server.utils

object IamResources {
    const val ORGANIZATION = "iam-organization"
    const val ACCOUNT = "iam-account"
    const val USER = "iam-user"
    const val POLICY = "iam-policy"
    const val RESOURCE = "iam-resource"
    const val ROLE = "iam-role"
    const val ACTION = "iam-action"
    const val CREDENTIAL = "iam-credential"
    const val SUB_ORGANIZATION = "iam-sub-organization"
    const val USER_LINK = "iam-user-link"

    val resourceMap: Map<String, String> =
        mapOf(
            "users" to USER,
            "resources" to RESOURCE,
            "policies" to POLICY,
            "actions" to ACTION,
            "credentials" to CREDENTIAL,
            "roles" to ROLE,
            "organizations" to ORGANIZATION,
            "accounts" to ACCOUNT,
            "sub_organizations" to SUB_ORGANIZATION,
            "user_links" to USER_LINK,
        )
}
