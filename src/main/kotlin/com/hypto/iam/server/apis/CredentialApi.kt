@file:Suppress("ThrowsCount")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.RouteOption
import com.hypto.iam.server.extensions.deleteWithPermission
import com.hypto.iam.server.extensions.getWithPermission
import com.hypto.iam.server.extensions.patchWithPermission
import com.hypto.iam.server.extensions.postWithPermission
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.service.CredentialService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import java.util.UUID
import org.koin.ktor.ext.inject

fun Route.credentialApi() {
    val gson: Gson by inject()
    val credentialService: CredentialService by inject()

    postWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_id}" +
                    "/users/{userId}/credentials",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ),
        "createCredential"
    ) {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val subOrganizationId = call.parameters["sub_organization_id"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val request = call.receive<CreateCredentialRequest>().validate()

        val response = credentialService.createCredential(
            organizationId,
            subOrganizationId,
            userId,
            request
                .validUntil
        )

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created
        )
    }

    deleteWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials/{id}",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/" +
                    "{userId}/credentials/{id}",
                resourceNameIndex = 6,
                resourceInstanceIndex = 7,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ),
        "deleteCredential",
    ) {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val subOrganizationId = call.parameters["sub_organization_id"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("id required"))

        val response = credentialService.deleteCredential(organizationId, subOrganizationId, userId, id)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    getWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials/{id}",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/" +
                    "{userId}/credentials/{id}",
                resourceNameIndex = 6,
                resourceInstanceIndex = 7,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ),
        "getCredential",
    ) {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val subOrganizationId = call.parameters["sub_organization_id"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("id required"))

        val credential = credentialService.getCredentialWithoutSecret(organizationId, subOrganizationId, userId, id)

        call.respondText(
            text = gson.toJson(credential),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    getWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/" +
                    "{userId}/credentials",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ),
        "listCredential",

    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationId = call.parameters["sub_organization_id"]
        val userId = call.parameters["userId"]!!

        val credentials = credentialService.listCredentials(organizationId, subOrganizationId, userId)

        call.respondText(
            text = gson.toJson(credentials),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    patchWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials/{id}",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/" +
                    "{userId}/credentials/{id}",
                resourceNameIndex = 6,
                resourceInstanceIndex = 7,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ),
        "updateCredential"
    ) {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val subOrganizationId = call.parameters["sub_organization_id"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("id required"))
        val request = call.receive<UpdateCredentialRequest>().validate()

        val response = credentialService.updateCredentialAndGetWithoutSecret(
            organizationId,
            subOrganizationId,
            userId,
            id,
            request.status,
            request.validUntil
        )

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}
