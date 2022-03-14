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
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.application.Application
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.mockkClass
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.text.Charsets.UTF_8
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

internal class CredentialApiKtTest : AutoCloseKoinTest() {
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
            }
        }

        declareMock<UserPoliciesRepo> {
            with(MockUserPoliciesStore(mockStore)) {
                mockInsert(this@declareMock)
                mockFetchByPrincipalHrn(this@declareMock)
            }
        }
    }

    @Nested
    @DisplayName("Create credential API tests")
    inner class CreateCredentialTest {
        @Test
        fun `without expiry - success`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Actual test
                val requestBody = CreateCredentialRequest()

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNull(responseBody.validUntil)
                    Assertions.assertEquals(responseBody.status, Credential.Status.active)
                    Assertions.assertNotNull(responseBody.secret)
                }
            }
        }

        @Test
        fun `with expiry - success`() {
            withTestApplication(Application::handleRequest) {

                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Actual test
                val expiry = LocalDateTime.now().plusDays(1)
                val requestBody = CreateCredentialRequest(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNotNull(responseBody.validUntil)
                    Assertions.assertEquals(
                        expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), responseBody.validUntil
                    )
                    Assertions.assertEquals(responseBody.status, Credential.Status.active)
                    Assertions.assertNotNull(responseBody.secret)
                }
            }
        }

        @Test
        fun `expiry date in past - failure`() {
            withTestApplication(Application::handleRequest) {

                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Actual test
                val now = LocalDateTime.now().minusDays(1)
                val requestBody = CreateCredentialRequest(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Delete credential API tests")
    inner class DeleteCredentialTest {
        @Test
        fun `delete existing credential`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Create a credential to delete
                val credentialsToDelete = MockCredentialsStore(mockStore).createCredential(createdUser.hrn)

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }

                // Validate that credential has been deleted
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                }

                // Validate that using deleted credentials returns 401
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${credentialsToDelete.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status())
                }
            }
        }

        @Test
        fun `credential not found`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                val nonExistentCredentialId = UUID.randomUUID().toString()

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/$nonExistentCredentialId"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
            }
        }

        @Test
        fun `unauthorized access`() {
            withTestApplication(Application::handleRequest) {
                val (organization1, user1, credentials1) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val (_, _, credentials2) = DataSetupHelper.createOrganizationUserCredential(this, mockStore)
                val user1Name = ResourceHrn(user1.hrn).resourceInstance

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${organization1.id}/users/$user1Name/credentials/${credentials1.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${credentials2.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Forbidden, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
            }
        }
    }

    @Nested
    @DisplayName("Get credential API tests")
    inner class GetCredentialTest {
        @Test
        fun `success - response does not have secret`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${createdCredentials.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNull(responseBody.validUntil)
                    Assertions.assertNull(responseBody.secret)
                    Assertions.assertEquals(Credential.Status.active, responseBody.status)
                }
            }
        }

        @Test
        fun `not found`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance
                val nonExistentCredentialId = UUID.randomUUID().toString()

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/$nonExistentCredentialId"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
            }
        }

        @Test
        fun `invalid credential id`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = DataSetupHelper
                    .createOrganizationUserCredential(this, mockStore)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/inValid_credential_id"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
            }
        }
    }
}
