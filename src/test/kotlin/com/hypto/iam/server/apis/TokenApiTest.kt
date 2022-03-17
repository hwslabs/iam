package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.helpers.MockCredentialsStore
import com.hypto.iam.server.helpers.MockOrganizationStore
import com.hypto.iam.server.helpers.MockPoliciesStore
import com.hypto.iam.server.helpers.MockStore
import com.hypto.iam.server.helpers.MockUserPoliciesStore
import com.hypto.iam.server.helpers.MockUserStore
import com.hypto.iam.server.models.ResourceAction
import com.hypto.iam.server.models.ResourceActionEffect
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.models.ValidationRequest
import com.hypto.iam.server.models.ValidationResponse
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IamResourceTypes
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
import io.mockk.mockkClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.inject
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock

class TokenApiTest : AutoCloseKoinTest() {
    private val gson = Gson()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(repositoryModule, controllerModule, applicationModule)
    }

    @JvmField
    @RegisterExtension
    val koinMockProvider = MockProviderExtension.create { mockkClass(it) }

    private val mockStore = MockStore()

    @AfterEach
    fun tearDown() {
        mockStore.clear()
    }

    @BeforeEach
    fun setUp() {
        declareMock<OrganizationRepo> {
            MockOrganizationStore(mockStore).let {
                it.mockInsert(this@declareMock)
                it.mockFindById(this@declareMock)
            }
        }

        declareMock<UserRepo> {
            with(MockUserStore(mockStore)) {
                mockFetchByHrn(this@declareMock)
                mockExistsById(this@declareMock)
                mockCreate(this@declareMock)
            }
        }

        declareMock<CredentialsRepo> {
            with(MockCredentialsStore(mockStore)) {
                mockFetchByRefreshToken(this@declareMock)
                mockCreate(this@declareMock)
                mockDelete(this@declareMock)
                mockFetchByIdAndUserHrn(this@declareMock)
            }
        }

        declareMock<PoliciesRepo> {
            with(MockPoliciesStore(mockStore)) {
                mockCreate(this@declareMock)
                mockExistsById(this@declareMock)
                mockExistsByIds(this@declareMock)
                mockFetchByHrn(this@declareMock)
                mockDeleteByHrn(this@declareMock)
            }
        }

        declareMock<UserPoliciesRepo> {
            with(MockUserPoliciesStore(mockStore)) {
                mockInsert(this@declareMock)
                mockFetchByPrincipalHrn(this@declareMock)
                mockFetchPoliciesByUserHrnPaginated(this@declareMock)
            }
        }
    }

    @Test
    fun `generate and validate action with token - without key rotation`() {
        withTestApplication(Application::handleRequest) {
            val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                .createOrganizationUserCredential(this, mockStore)

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/token"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
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
                                                organization = createdOrganization.id,
                                                resource = IamResourceTypes.USER,
                                                resourceInstance = createdUser.hrn
                                            ).toString(),
                                            ActionHrn(
                                                organization = createdOrganization.id,
                                                resource = IamResourceTypes.USER,
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
            val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                .createOrganizationUserCredential(this, mockStore)

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/token"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
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
                                                organization = createdOrganization.id,
                                                resource = IamResourceTypes.USER,
                                                resourceInstance = createdUser.hrn
                                            ).toString(),
                                            ActionHrn(
                                                organization = createdOrganization.id,
                                                resource = IamResourceTypes.USER,
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
