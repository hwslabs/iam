package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.plugins.inject
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mu.KotlinLogging

/**
 * API to create & delete organization in IAM.
 * NOTE: These apis are restricted. Clients are forbidden to use this api to create/delete organizations.
 * Only users (Just "hypto-root" at the moment) having access to master key can use this api.
 */
fun Route.createAndDeleteOrganizationApi() {
    val service: OrganizationsService by inject()
    val gson: Gson by inject()

    route("/organizations") {
        post {
            val request = call.receive<CreateOrganizationRequest>().validate()
            val (organization, credential) = service.createOrganization(
                request.name,
                description = "",
                rootUser = request.rootUser
            )
            call.respondText(
                text = gson.toJson(CreateOrganizationResponse(organization, credential)),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }

        delete("/{id}") {
            val id = call.parameters["id"]!!
            val response = service.deleteOrganization(id)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
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
        withPermission("getOrganization") {
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

        withPermission("updateOrganization") {
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
