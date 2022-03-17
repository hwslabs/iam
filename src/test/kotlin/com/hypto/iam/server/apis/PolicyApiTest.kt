package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.helpers.MockStore
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyStatement
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
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

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

    @Nested
    @DisplayName("Create policy API tests")
    inner class CreatePolicyTest {
        @Test
        fun `happy case`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, _) = DataSetupHelper
                    .createOrganizationUserCredential(this)

                val createdOrganization = createdOrganizationResponse.organization!!
                val createdCredentials = createdOrganizationResponse.adminUserCredential!!

                val policyName = "SamplePolicy"
                val policyStatements = listOf(PolicyStatement("resource", "action", PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
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

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }
    }
}
