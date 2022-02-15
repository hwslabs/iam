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
import io.ktor.locations.post
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.tokenApi() {
    post { _: Paths.GetToken ->
        var principal = ""
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            // 1. Validate user
            // 2. Call TokenService.generateJwtToken(userId, orgId) to get signed token
            // 3. Return the token
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}
