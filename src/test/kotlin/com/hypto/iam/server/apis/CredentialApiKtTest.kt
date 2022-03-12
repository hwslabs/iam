package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.MockCredentialsStore
import com.hypto.iam.server.helpers.MockOrganizationStore
import com.hypto.iam.server.helpers.MockStore
import com.hypto.iam.server.helpers.MockUserStore
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.application.Application
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.charsets.Charset
import io.mockk.mockkClass
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
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
    private val rootToken = "hypto-root-secret-key"

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
            MockUserStore(mockStore).mockFetchByHrn(this@declareMock)
        }

        declareMock<CredentialsRepo> {
            MockCredentialsStore(mockStore).let {
                it.mockFetchByRefreshToken(this@declareMock)
                it.mockCreate(this@declareMock)
                it.mockDelete(this@declareMock)
                it.mockFetchByIdAndUserHrn(this@declareMock)
            }
        }
    }

    fun createOrganizationUserCredential(
        engine: TestApplicationEngine
    ): Triple<Organization, Users, CredentialsRecord> {
        with(engine) {
            // Create organization
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            "testName",
                            AdminUser(
                                username = "testAdminUser",
                                passwordHash = "#123",
                                email = "testAdminUser@example.com",
                                phone = ""
                            )
                        )
                    )
                )
            }
            val createdOrganization = gson
                .fromJson(createOrganizationCall.response.content, Organization::class.java)
            val userName = "testUserName"

            // Create User
            // TODO: Replace createUser method call with API call
            val createdUser = MockUserStore(mockStore).createUser(createdOrganization.id, userName)

            // Create a Credential to call create credential API with
            // TODO: Replace this call with `/login` API once it's implemented so that
            //  the createCredential can be called with the returned JWT token
            val createdCredentials = MockCredentialsStore(mockStore).createCredential(createdUser.hrn)

            return Triple(createdOrganization, createdUser, createdCredentials)
        }
    }

    @Nested
    @DisplayName("Create credential API tests")
    inner class CreateCredentialTest {
        @Test
        fun `without expiry - success`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Actual test
                val requestBody = CreateCredentialRequest()

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credential"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(Json.withCharset(Charset.defaultCharset()), response.contentType())

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

                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Actual test
                val expiry = LocalDateTime.now().plusDays(1)
                val requestBody = CreateCredentialRequest(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credential"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(Charset.defaultCharset()),
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

                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Actual test
                val now = LocalDateTime.now().minusDays(1)
                val requestBody = CreateCredentialRequest(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credential"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(Charset.defaultCharset()),
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
                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                // Create a credential to delete
                val credentialsToDelete = MockCredentialsStore(mockStore).createCredential(createdUser.hrn)

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credential/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(Charset.defaultCharset()), response.contentType())
                }

                // Validate that credential has been deleted
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credential/${credentialsToDelete.id}"
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
                        "/organizations/${createdOrganization.id}/users/$userName/credential/${credentialsToDelete.id}"
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
                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                val nonExistentCredentialId = UUID.randomUUID().toString()

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credential/$nonExistentCredentialId"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(Json.withCharset(Charset.defaultCharset()), response.contentType())
                }
            }
        }

        @Test
        fun `unauthorized access`() {
            withTestApplication(Application::handleRequest) {
                val (organization1, user1, credentials1) = createOrganizationUserCredential(this)
                val (_, _, credentials2) = createOrganizationUserCredential(this)
                val user1Name = ResourceHrn(user1.hrn).resourceInstance

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${organization1.id}/users/$user1Name/credential/${credentials1.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${credentials2.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Forbidden, response.status())
                    Assertions.assertEquals(Json.withCharset(Charset.defaultCharset()), response.contentType())
                }
            }
        }
    }

    @Nested
    @DisplayName("Get credential API tests")
    inner class GetCredentialTest {
        @Test
        fun `success - without refresh token`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance

                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credential/${createdCredentials.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(Charset.defaultCharset()), response.contentType())

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNull(responseBody.validUntil)
                    Assertions.assertEquals(responseBody.status, Credential.Status.active)
                }
            }
        }

        @Test
        fun `not found`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance
            }
        }

        @Test
        fun `invalid credential id`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser, createdCredentials) = createOrganizationUserCredential(this)
                val userName = ResourceHrn(createdUser.hrn).resourceInstance
            }
        }
    }
}
