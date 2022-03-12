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
import com.hypto.iam.server.helpers.MockCredentialsStore
import com.hypto.iam.server.helpers.MockOrganizationStore
import com.hypto.iam.server.helpers.MockPoliciesStore
import com.hypto.iam.server.helpers.MockStore
import com.hypto.iam.server.helpers.MockUserPoliciesStore
import com.hypto.iam.server.helpers.MockUserStore
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.Organization
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
import io.ktor.utils.io.charsets.Charset
import io.mockk.mockkClass
import kotlin.test.assertFalse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock

internal class OrganizationApiKtTest : AutoCloseKoinTest() {
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
            with(MockOrganizationStore(mockStore)) {
                mockInsert(this@declareMock)
                mockFindById(this@declareMock)
            }
        }

        declareMock<CredentialsRepo> {
            MockCredentialsStore(mockStore).mockFetchByRefreshToken(this@declareMock)
        }
        declareMock<UserRepo> {
            with(MockUserStore(mockStore)) {
                mockFetchByHrn(this@declareMock)
                mockExistsById(this@declareMock)
                mockCreate(this@declareMock)
            }
        }

        declareMock<PoliciesRepo> {
            with(MockPoliciesStore(mockStore)) {
                mockCreate(this@declareMock)
                mockExistsById(this@declareMock)
                mockExistsByIds(this@declareMock)
            }
        }

        declareMock<UserPoliciesRepo> {
            MockUserPoliciesStore(mockStore).mockInsert(this@declareMock)
        }
    }

    @Test
    fun `create organization with valid root credentials`() {
        withTestApplication(Application::handleRequest) {
            val requestBody = CreateOrganizationRequest("testName", AdminUser("testPassword",
                "testEmail", "testPhone", "testUserName"))
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            ) {
                val responseBody = gson.fromJson(response.content, Organization::class.java)

                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charset.defaultCharset()), response.contentType())

                assertEquals(requestBody.name, responseBody.name)
                assertEquals(10, responseBody.id.length)
            }
        }
    }

    @Test
    fun `create organization with invalid root credentials`() {
        withTestApplication(Application::handleRequest) {
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", "bad creds")
                    setBody(gson.toJson(CreateOrganizationRequest("testName", AdminUser("testPassword",
                        "testEmail", "testPhone", "testUserName"))))
                }
            ) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertFalse(response.headers.contains(HttpHeaders.ContentType))
                assertEquals(null, response.content)
            }
        }
    }

    @Test
    fun `get organization with invalid credentials`() {
        withTestApplication(Application::handleRequest) {

            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(gson.toJson(CreateOrganizationRequest("testName", AdminUser("testPassword",
                    "testEmail", "testPhone", "testUserName"))))
            }
            val createdOrganization = gson.fromJson(createOrganizationCall.response.content, Organization::class.java)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${createdOrganization.id}") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer test-bearer-token")
                }
            ) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertFalse(response.headers.contains(HttpHeaders.ContentType))
                assertEquals(null, response.content)
            }
        }
    }

    @Test
    fun `get organization success`() {
        withTestApplication(Application::handleRequest) {

            // Create organization
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            "testName",
                            AdminUser("testPassword", "testEmail", "testPhone", "testUserName")
                        )
                    )
                )
            }
            val createdOrganization = gson.fromJson(createOrganizationCall.response.content, Organization::class.java)

            // Create User
            // TODO: Replace createUser method call with API call
            val createdUser = MockUserStore(mockStore).createUser(createdOrganization.id, "testUserName")

            // Create Credential
            // TODO: Replace createCredential method call with API call
            val createdCredentials = MockCredentialsStore(mockStore).createCredential(createdUser.hrn)

//            Cannot create credential without an existing credentials ;)
//            val createCredentialCall = handleRequest(
//                HttpMethod.Get,
//                "/organizations/${createdOrganization.id}/users/testUserName/credential"
//            ) {
//                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
//                addHeader(HttpHeaders.Authorization, "Bearer test-bearer-token")
//            }

            with(
                handleRequest(HttpMethod.Get, "/organizations/${createdOrganization.id}") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charset.defaultCharset()), response.contentType())

                val fetchedOrganization = gson.fromJson(response.content, Organization::class.java)
                assertEquals(createdOrganization, fetchedOrganization)
            }
        }
    }

    @Test
    fun `get organization not found`() {
        withTestApplication(Application::handleRequest) {

            // Create organization
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(gson.toJson(CreateOrganizationRequest("testName", AdminUser("testPassword",
                    "testEmail", "testPhone", "testUserName"))))
            }
            val createdOrganization = gson.fromJson(createOrganizationCall.response.content, Organization::class.java)

            // Create User
            // TODO: Replace createUser method call with API call
            val createdUser = MockUserStore(mockStore).createUser(createdOrganization.id, "testUserName")

            // Create Credential
            // TODO: Replace createCredential method call with API call
            val createdCredentials = MockCredentialsStore(mockStore).createCredential(createdUser.hrn)

            with(
                handleRequest(HttpMethod.Get, "/organizations/onvalidOrganizationId") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                }
            ) {
                // These assertions
                assertEquals(HttpStatusCode.NotFound, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charset.defaultCharset()),
                    response.contentType()
                )
            }
        }
    }
}
