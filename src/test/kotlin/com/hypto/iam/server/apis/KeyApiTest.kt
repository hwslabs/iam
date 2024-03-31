package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.models.KeyResponse
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.service.TokenServiceImpl
import io.jsonwebtoken.Jwts
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.koin.test.inject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class KeyApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `validate token using public key`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()

            val createTokenCall =
                client.post("/organizations/${organizationResponse.organization.id}/token") {
                    header(HttpHeaders.ContentType, Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${organizationResponse.rootUserToken}")
                }
            val token =
                gson
                    .fromJson(createTokenCall.bodyAsText(), TokenResponse::class.java).token

            println(token)

            val splitToken: Array<String> = token.split(".").toTypedArray()
            val unsignedToken = splitToken[0] + "." + splitToken[1] + "."
            val jwt = Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken)

            val kid = jwt.header.getValue(TokenServiceImpl.KEY_ID) as String

            val response =
                client.get(
                    "/keys/$kid?format=der",
                ) {
                    header(HttpHeaders.Authorization, "Bearer ${organizationResponse.rootUserToken}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                Json,
                response.contentType(),
            )
            val publicKeyResponse = gson.fromJson(response.bodyAsText(), KeyResponse::class.java)

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
