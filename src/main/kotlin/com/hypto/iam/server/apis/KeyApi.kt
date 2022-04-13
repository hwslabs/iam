package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.PublicKeyResponse
import com.hypto.iam.server.service.MasterKey
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import java.util.Base64
import org.koin.ktor.ext.inject

fun Route.keyApi() {
    val gson: Gson by inject()

    get("/public_keys/{kid}") {
        val kid = call.parameters["kid"]
        val format = call.request.queryParameters["format"] ?: "der"

        val masterKey = MasterKey.of(kid!!)
        val publicKey = when (format) {
            "der" -> masterKey.publicKeyDer
            "pem" -> masterKey.publicKeyPem
            else -> {
                throw IllegalArgumentException("Invalid format")
            }
        }

        val response = PublicKeyResponse(kid, Base64.getEncoder().encodeToString(publicKey))

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}
