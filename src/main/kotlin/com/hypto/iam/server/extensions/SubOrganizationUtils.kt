package com.hypto.iam.server.extensions

import io.ktor.server.plugins.BadRequestException
import java.util.Base64

/**
 * For sub organizations, we have to encode the email address to include the org and sub org id details in the email
 * so that users can have unique credentials across orgs and sub orgs.
 *
 * To support this, we are using the email local addressing scheme. This scheme allows us to add a suffix to email
 * address.
 * Ex: hello@hypto.in can be encoded as hello+<base64(orgId:subOrgId)>@hypto.in
 *
 * With this option, same email address hello@hypto.in can coonfigure two different passwords for sub orgId1 and sub
 * orgId2.
 */
fun getEncodedEmail(
    organizationId: String,
    subOrganizationName: String?,
    email: String,
) =
    if (subOrganizationName != null) {
        encodeSubOrgUserEmail(
            email,
            organizationId,
        )
    } else {
        email
    }

private fun encodeSubOrgUserEmail(
    email: String,
    organizationId: String,
): String {
    val emailParts = email.split("@").takeIf { it.size == 2 } ?: throw BadRequestException("Invalid email address")
    val localPart = emailParts[0]
    val domainPart = emailParts[1]
    val subAddress = Base64.getEncoder().encodeToString(organizationId.toByteArray())
    return "$localPart+$subAddress@$domainPart"
}
