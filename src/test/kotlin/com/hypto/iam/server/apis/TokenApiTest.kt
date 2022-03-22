package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.ResourceAction
import com.hypto.iam.server.models.ResourceActionEffect
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.models.ValidationRequest
import com.hypto.iam.server.models.ValidationResponse
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.inject

class TokenApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `generate and validate action with token - without key rotation`() {
        withTestApplication(Application::handleRequest) {
            val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/token"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdOrganization.adminUserCredential?.secret}")
                }
            ) {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                val responseBody = gson.fromJson(response.content, TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/validate"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                        setBody(
                            gson.toJson(
                                ValidationRequest(
                                    listOf(
                                        ResourceAction(
                                            ResourceHrn(
                                                organization = createdOrganization.organization!!.id,
                                                resource = IamResources.USER,
                                                resourceInstance = createdUser.username
                                            ).toString(),
                                            ActionHrn(
                                                organization = createdOrganization.organization!!.id,
                                                resource = IamResources.USER,
                                                action = "createCredentials"
                                            ).toString()
                                        )
                                    )
                                )
                            )
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                    validationResponseBody.results.forEach {
                        Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                    }
                }
            }
        }
    }

    @Test
    fun `generate token and validate action after key rotation`() {
        withTestApplication(Application::handleRequest) {
            val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/token"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdOrganization.adminUserCredential?.secret}")
                }
            ) {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                val responseBody = gson.fromJson(response.content, TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                // TODO: Expose key rotation as an API and invoke it
                val masterKeysRepo by inject<MasterKeysRepo>()
                masterKeysRepo.rotateKey()

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/validate"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                        setBody(
                            gson.toJson(
                                ValidationRequest(
                                    listOf(
                                        ResourceAction(
                                            ResourceHrn(
                                                organization = createdOrganization.organization!!.id,
                                                resource = IamResources.USER,
                                                resourceInstance = createdUser.username
                                            ).toString(),
                                            ActionHrn(
                                                organization = createdOrganization.organization!!.id,
                                                resource = IamResources.USER,
                                                action = "createCredentials"
                                            ).toString()
                                        )
                                    )
                                )
                            )
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                    validationResponseBody.results.forEach {
                        Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                    }
                }
            }
        }
    }
}
