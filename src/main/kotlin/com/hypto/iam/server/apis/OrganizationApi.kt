package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.validators.validate
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import mu.KotlinLogging
import org.koin.ktor.ext.inject

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
            val response = service.createOrganization(request.name, description = "", adminUser = request.adminUser)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }

        delete("/{id}") {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}

private val logger = KotlinLogging.logger { }

/**
 * Route to get and update organizations in IAM
 */
fun Route.getAndUpdateOrganizationApi() {
    val controller: OrganizationsService by inject()
    val gson: Gson by inject()

    route("/organizations/{id}") {
        get {
            val id = call.parameters["id"]
                ?: throw IllegalArgumentException("Required id to get the Organization details")
            val response = controller.getOrganization(id)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }

        patch {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}
