package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mu.KotlinLogging
import org.koin.ktor.ext.inject

/**
 * API to create organization in IAM.
 */
fun Route.createOrganizationApi() {
    val service: OrganizationsService by inject()
    val gson: Gson by inject()

    post("/organizations") {
        val request = call.receive<CreateOrganizationRequest>().validate()
        val passcodeStr = call.principal<ApiPrincipal>()?.tokenCredential?.value
        val (organization, tokenResponse) = service.createOrganization(
            request.name,
            description = request.description ?: "",
            rootUser = request.rootUser,
            passcodeStr = passcodeStr
        )
        call.respondText(
            text = gson.toJson(CreateOrganizationResponse(organization, tokenResponse.token)),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created
        )
    }
}

/**
 * API to delete organization in IAM.
 * NOTE: These apis are restricted. Clients are forbidden to use this api to delete organizations.
 * Only users (Just "hypto-root" at the moment) having access to master key can use this api.
 */
fun Route.deleteOrganizationApi() {
    val service: OrganizationsService by inject()
    val gson: Gson by inject()

    delete("/organizations/{id}") {
        val id = call.parameters["id"]!!
        val response = service.deleteOrganization(id)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

private val logger = KotlinLogging.logger { }

/**
 * Route to get and update organizations in IAM
 */
fun Route.getAndUpdateOrganizationApi() {
    val service: OrganizationsService by inject()
    val gson: Gson by inject()

    route("/organizations/{id}") {
        withPermission(
            "getOrganization",
            getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1)
        ) {
            get {
                val id = call.parameters["id"]!!
                val response = service.getOrganization(id)
                call.respondText(
                    text = gson.toJson(response),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }

        withPermission(
            "updateOrganization",
            getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1)
        ) {
            patch {
                val id = call.parameters["id"]!!
                val request = call.receive<UpdateOrganizationRequest>().validate()
                val response =
                    service.updateOrganization(id = id, name = request.name, description = request.description)
                call.respondText(
                    text = gson.toJson(response),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }
    }
}
