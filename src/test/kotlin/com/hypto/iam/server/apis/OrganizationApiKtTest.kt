package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.service.OrganizationsService
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.mockkClass
import java.time.LocalDateTime
import java.util.*
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

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(repositoryModule, controllerModule, applicationModule)
    }

    @JvmField
    @RegisterExtension
    val koinMockProvider = MockProviderExtension.create { mockkClass(it) }

    @BeforeEach
    fun setUp() {
        declareMock<OrganizationsService> {
            coEvery { this@declareMock.createOrganization(any(), any()) } coAnswers {
                Organization("testId", firstArg(), "")
            }
            coEvery { this@declareMock.getOrganization(any()) } coAnswers {
                Organization(firstArg(), "testName", "testDescription")
            }
        }
        declareMock<CredentialsRepo> {
            coEvery { this@declareMock.fetchByRefreshToken(any()) } coAnswers {
                CredentialsRecord(UUID.randomUUID(),
                    LocalDateTime.MAX,
                    "Active",
                    "testRefreshToken",
                    "hrn:testOrg::iam-user/testId",
                    LocalDateTime.MAX, LocalDateTime.MAX)
            }
        }
        declareMock<UserRepo> {
            coEvery { this@declareMock.fetchByHrn(any()) } coAnswers {
                Users("hrn:testOrg::iam-user/testId",
                    "testSaltedPassword",
                    "testEmail",
                    "testPhone",
                    true,
                    "testUserType",
                    "Active",
                    UUID.randomUUID(),
                    "hrn:::iam-organization/testOrg",
                    LocalDateTime.MAX, LocalDateTime.MAX)
            }
        }
    }

    @Test
    fun `create organization with valid creds`() {
        withTestApplication(Application::handleRequest) {
            with(handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", "hypto-root-secret-key")
                setBody(gson.toJson(CreateOrganizationRequest("testName")))
            }) {
                val expected = gson.toJson(Organization("testId", "testName", ""))
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(expected, response.content)
            }
        }
    }

    @Test
    fun `create organization with invalid creds`() {
        withTestApplication(Application::handleRequest) {
            with(handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", "bad creds")
                setBody(gson.toJson(CreateOrganizationRequest("testName")))
            }) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertEquals(null, response.content)
            }
        }
    }

    @Test
    fun `get organization success`() {
        withTestApplication(Application::handleRequest) {
            with(handleRequest(HttpMethod.Get, "/organizations/testId") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer test-bearer-token")
                setBody(gson.toJson(CreateOrganizationRequest("testName")))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(gson.toJson(Organization("testId", "testName", "testDescription")), response.content)
            }
        } }
}
