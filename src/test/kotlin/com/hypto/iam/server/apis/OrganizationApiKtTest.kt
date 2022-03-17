package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.MockStore
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.utils.IdGenerator
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
import kotlin.test.assertFalse
import kotlin.text.Charsets.UTF_8
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
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

    companion object {
        @JvmStatic
        @Container
        private val container =
            PostgreSQLContainer("postgres:14.1-alpine")
                .withDatabaseName("iam")
                .withUsername("root")
                .withPassword("password")
                .withReuse(true)

        @JvmStatic
        @BeforeAll
        fun setUp() {

            container.start()
            val configuration = ClassicConfiguration()
            configuration.setDataSource(container.jdbcUrl, container.username, container.password)
            configuration.setLocations(Location("filesystem:src/main/resources/db/migration"))
            val flyway = Flyway(configuration)
            flyway.migrate()

            System.setProperty("config.override.database.name", "iam")
            System.setProperty("config.override.database.user", "root")
            System.setProperty("config.override.database.password", "password")
            System.setProperty("config.override.database.host", container.host)
            System.setProperty("config.override.database.port", container.firstMappedPort.toString())
        }
    }

    @BeforeEach
    fun setUpBeforeEach() {
        mockStore.clear()
    }

    @AfterEach
    fun tearDown() {
        mockStore.clear()
    }

    @Test
    fun `create organization with valid root credentials`() {

        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            lateinit var orgId: String
            val requestBody = CreateOrganizationRequest(
                orgName,
                AdminUser(
                    userName,
                    "testEmail", "testPhone", "testUserName"
                )
            )
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            ) {
                val responseBody = gson.fromJson(response.content, Organization::class.java)
                orgId = responseBody.id
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                assertEquals(requestBody.name, responseBody.name)
                assertEquals(10, responseBody.id.length)
            }
        }
    }

    @Test
    fun `create organization with invalid root credentials`() {
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", "bad creds")
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                AdminUser(
                                    userName,
                                    "testEmail", "testPhone", "testUserName"
                                )
                            )
                        )
                    )
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
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            AdminUser(userName, "testPassword", "testEmail", "testPhone")
                        )
                    )
                )
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
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            AdminUser(userName, "testPassword", "testEmail", "testPhone")
                        )
                    )
                )
            }
            val createdOrganization = gson.fromJson(createOrganizationCall.response.content, Organization::class.java)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${createdOrganization.id}") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
//                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredential.secret}")
                    addHeader("X-Api-Key", rootToken)
                }

            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val fetchedOrganization = gson.fromJson(response.content, Organization::class.java)
                assertEquals(createdOrganization, fetchedOrganization)
            }
        }
    }

    @Test
    fun `get organization not found`() {
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            // Create organization
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            AdminUser(userName, "testPassword", "testEmail", "testPhone")
                        )
                    )
                )
            }
            val createdOrganization = gson.fromJson(createOrganizationCall.response.content, Organization::class.java)

            with(
                handleRequest(HttpMethod.Get, "/organizations/inValidOrganizationId") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
//                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.refreshToken}")
                    addHeader("X-Api-Key", rootToken)
                }

            ) {
                // These assertions
                assertEquals(HttpStatusCode.NotFound, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(UTF_8),
                    response.contentType()
                )
            }
        }
    }
}
