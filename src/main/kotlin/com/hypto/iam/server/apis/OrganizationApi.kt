/**
* Hypto IAM
* APIs for Hypto IAM Service.
*
* OpenAPI spec version: 1.0.0
* Contact: engineering@hypto.in
*
* NOTE: This class is auto generated by the swagger code generator program.
* https://github.com/swagger-api/swagger-codegen.git
* Do not edit the class manually.
*/package com.hypto.iam.server.apis

import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.models.CreateOrganizationRequest
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import java.time.LocalDateTime
import org.koin.ktor.ext.inject

/**
 * API to create organization in IAM.
 * NOTE: This api is restricted. Clients are forbidden to use this api to create organizations.
 * Only "hypto-root" user having access to the secret can use this api.
 */
fun Route.createOrganizationApi() {
    val repo: OrganizationRepo by inject()

    post<CreateOrganizationRequest>("/organizations") {
        assert(call.authentication.principal != null) { "Unable to validate principal making the request." }
        var id = repo.insert(Organizations("aaa", it.name, null, LocalDateTime.now(), LocalDateTime.now()))
        call.respondText("{success: true, id: $id}", ContentType.Application.Json)
    }
}

fun Route.organizationApi() {
    val repo: OrganizationRepo by inject()
    delete("/organizations/{id}") {
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }

    get("/organizations/{id}") {
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {

            val org = OrganizationRepo.fetchOneById("a")
            println(org.toString())
//            call.respond()

            call.respond(HttpStatusCode.NotImplemented)
        }
    }
    patch("/organizations/{id}") {
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}
