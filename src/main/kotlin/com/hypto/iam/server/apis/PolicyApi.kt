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

import com.hypto.iam.server.Paths
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.delete
import io.ktor.locations.get
import io.ktor.locations.patch
import io.ktor.locations.post
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.policyApi() {

    post { _: Paths.CreatePolicy ->
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
    delete { _: Paths.DeletePolicy ->
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
    get { _: Paths.GetPolicy ->
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
    get { _: Paths.GetUserPolicies ->
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
    patch { _: Paths.UpdatePolicy ->
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}
