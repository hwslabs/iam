package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.CredentialsRepo
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
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.utils.HrnFactory
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock

class PolicyApiTest : AutoCloseKoinTest() {
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
            MockCredentialsStore(mockStore).let {
                it.mockFetchByRefreshToken(this@declareMock)
                it.mockCreate(this@declareMock)
                it.mockDelete(this@declareMock)
                it.mockFetchByIdAndUserHrn(this@declareMock)
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

    @Nested
    @DisplayName("Create policy API tests")
    inner class CreatePolicyTest {
        @Test
        fun `valid policy - success`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                val policyName = "SamplePolicy"
                val policyStatements = listOf(PolicyStatement("resource", "action", PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, Policy::class.java)
                    Assertions.assertEquals(createdOrganization.id, responseBody.organizationId)

                    val expectedPolicyHrn = ResourceHrn(
                        organization = createdOrganization.id,
                        resource = IamResourceTypes.POLICY,
                        account = null,
                        resourceInstance = policyName
                    )

                    Assertions.assertEquals(expectedPolicyHrn.toString(), responseBody.hrn)
                    Assertions.assertEquals(policyName, responseBody.name)
                    Assertions.assertEquals(1, responseBody.version)
                    Assertions.assertEquals(policyStatements, responseBody.statements)
                }
            }
        }

        @Test
        fun `policy name already in use`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                val policyName = "SamplePolicy"
                val policyStatements = listOf(PolicyStatement("resource", "action", PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createdOrganization.id}/policies"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    setBody(gson.toJson(requestBody))
                }

                // Act
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    // Asset
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `policy without statements - failure`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                val policyName = "SamplePolicy"
                val requestBody = CreatePolicyRequest(policyName, listOf())

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `policy with too many policy statements - failure`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                val policyName = "SamplePolicy"
                val policyStatements = mutableListOf<PolicyStatement>()

                val requestBody = CreatePolicyRequest(
                    policyName,
                    (0..50).map {
                        PolicyStatement("resource$it", "action$it", PolicyStatement.Effect.allow)
                    }
                )

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Get policy API tests")
    inner class GetPolicyTest {
        @Test
        fun `existing policy - success`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                val policyName = "SamplePolicy"
                val policyStatements = listOf(PolicyStatement("resource", "action", PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createdOrganization.id}/policies"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    setBody(gson.toJson(requestBody))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.response.content, Policy::class.java)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/policies/${createdPolicy.name}"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, Policy::class.java)
                    Assertions.assertEquals(createdOrganization.id, responseBody.organizationId)

                    val expectedPolicyHrn = ResourceHrn(
                        organization = createdOrganization.id,
                        resource = IamResourceTypes.POLICY,
                        account = null,
                        resourceInstance = policyName
                    )

                    Assertions.assertEquals(expectedPolicyHrn.toString(), responseBody.hrn)
                    Assertions.assertEquals(policyName, responseBody.name)
                    Assertions.assertEquals(1, responseBody.version)
                    Assertions.assertEquals(policyStatements, responseBody.statements)
                }
            }
        }

        @Test
        fun `non existing policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Delete policy API tests")
    inner class DeletePolicyTest {
        @Test
        fun `delete existing policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                val policyName = "SamplePolicy"
                val policyStatements = listOf(PolicyStatement("resource", "action", PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createdOrganization.id}/policies"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    setBody(gson.toJson(requestBody))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.response.content, Policy::class.java)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/policies/${createdPolicy.name}"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `delete non existent policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganization, _, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/policies/non_existent+policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Get policies attached to user API test")
    inner class GetPoliciesByUserTest {
        @Test
        fun `get policies of a user`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)

                (1..5).forEach {
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        val policyStatements = listOf(
                            PolicyStatement("resource", "action", PolicyStatement.Effect.allow)
                        )
                        val requestBody = CreatePolicyRequest("SamplePolicy$it", policyStatements)
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                }

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/" +
                            "${(HrnFactory.getHrn(createdUser.hrn) as ResourceHrn).resourceInstance}" +
                            "/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

//                    val results = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)
                    println(response.content)
                }
            }
        }
    }
}
