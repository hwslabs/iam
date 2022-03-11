package com.hypto.iam.server.helpers

import com.google.gson.Gson
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.models.User
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody

object DataSetupHelper {
    private val gson = Gson()
    private const val rootToken = "hypto-root-secret-key"

    fun createOrganizationUserCredential(
        engine: TestApplicationEngine,
        mockStore: MockStore
    ): Triple<Organization, User, Credential> {
        with(engine) {
            // Create organization
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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
            val createOrganizationResponse = gson
                .fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)
            val organizationId = createOrganizationResponse.organization!!.id

            // Get admin user
            val getUserCall = handleRequest(HttpMethod.Get, "/organizations/$organizationId/users/testAdminUser") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createOrganizationResponse
                    .adminUserCredential!!.secret}")
            }
            val user = gson.fromJson(getUserCall.response.content, User::class.java)
            return Triple(createOrganizationResponse.organization!!, user,
                createOrganizationResponse.adminUserCredential!!)
        }
    }
}
