package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.KeyResponse
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.service.TokenServiceImpl
import io.jsonwebtoken.Jwts
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.koin.test.inject

class KeyApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `validate token using public key`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)

            val createTokenCall =
                handleRequest(HttpMethod.Post, "/organizations/${organizationResponse.organization?.id}/token") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${organizationResponse.rootUserToken}")
                }
            val token = gson
                .fromJson(createTokenCall.response.content, TokenResponse::class.java).token

            println(token)

            val splitToken: Array<String> = token.split(".").toTypedArray()
            val unsignedToken = splitToken[0] + "." + splitToken[1] + "."
            val jwt = Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken)

            val kid = jwt.header.getValue(TokenServiceImpl.KEY_ID) as String

            with(
                handleRequest(
                    HttpMethod.Get,
                    "/keys/$kid?format=der"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${organizationResponse.rootUserToken}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                val publicKeyResponse = gson.fromJson(response.content, KeyResponse::class.java)

                assertEquals(KeyResponse.Format.der, publicKeyResponse.format)

                val encodedPublicKey = Base64.getDecoder().decode(publicKeyResponse.key)
                val keyFactory = KeyFactory.getInstance("EC")
                val keySpec = X509EncodedKeySpec(encodedPublicKey)
                val publicKey = keyFactory.generatePublic(keySpec) as PublicKey

                assertEquals(kid, publicKeyResponse.kid)
                assertDoesNotThrow {
                    Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token)
                }
            }
        }
    }
}
