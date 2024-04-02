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
import org.koin.ktor.ext.inject
import java.util.UUID

const val USER_ID_REQUIRED = "userId required"
const val ID_REQUIRED = "id required"

fun getRoutOptionsForGetPatchDeleteCredentials(): List<RouteOption> {
    return listOf(
        RouteOption(
            "/organizations/{organization_id}/users/{userId}/credentials/{id}",
            resourceNameIndex = 2,
            resourceInstanceIndex = 3,
            organizationIdIndex = 1,
        ),
        RouteOption(
            "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users/" +
                "{userId}/credentials/{id}",
            resourceNameIndex = 4,
            resourceInstanceIndex = 5,
            organizationIdIndex = 1,
            subOrganizationNameIndex = 3,
        ),
    )
}

fun Route.credentialApi() {
    val gson: Gson by inject()
    val credentialService: CredentialService by inject()

    postWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}" +
                    "/users/{userId}/credentials",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "createCredential",
    ) {
        val organizationId =
            call.parameters["organization_id"]
                ?: throw IllegalArgumentException(ORGANIZATION_ID_REQUIRED)
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException(USER_ID_REQUIRED)
        val request = call.receive<CreateCredentialRequest>().validate()

        val response =
            credentialService.createCredential(
                organizationId,
                subOrganizationName,
                userId,
                request
                    .validUntil,
            )

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created,
        )
    }

    deleteWithPermission(
        getRoutOptionsForGetPatchDeleteCredentials(),
        "deleteCredential",
    ) {
        val organizationId =
            call.parameters["organization_id"]
                ?: throw IllegalArgumentException(ORGANIZATION_ID_REQUIRED)
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException(USER_ID_REQUIRED)
        val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException(ID_REQUIRED))

        val response = credentialService.deleteCredential(organizationId, subOrganizationName, userId, id)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    getWithPermission(
        getRoutOptionsForGetPatchDeleteCredentials(),
        "getCredential",
    ) {
        val organizationId =
            call.parameters["organization_id"]
                ?: throw IllegalArgumentException(ORGANIZATION_ID_REQUIRED)
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException(USER_ID_REQUIRED)
        val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException(ID_REQUIRED))

        val credential = credentialService.getCredentialWithoutSecret(organizationId, subOrganizationName, userId, id)

        call.respondText(
            text = gson.toJson(credential),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    getWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{userId}/credentials",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users/" +
                    "{userId}/credentials",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "listCredential",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["userId"]!!

        val credentials = credentialService.listCredentials(organizationId, subOrganizationName, userId)

        call.respondText(
            text = gson.toJson(credentials),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    patchWithPermission(
        getRoutOptionsForGetPatchDeleteCredentials(),
        "updateCredential",
    ) {
        val organizationId =
            call.parameters["organization_id"]
                ?: throw IllegalArgumentException(ORGANIZATION_ID_REQUIRED)
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException(USER_ID_REQUIRED)
        val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException(ID_REQUIRED))
        val request = call.receive<UpdateCredentialRequest>().validate()

        val response =
            credentialService.updateCredentialAndGetWithoutSecret(
                organizationId,
                subOrganizationName,
                userId,
                id,
                request.status,
                request.validUntil,
            )

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }
}
