package com.hypto.iam.server.helpers

import com.google.gson.Gson
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.extensions.usersFrom
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.Organization
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
    ): Triple<Organization, Users, CredentialsRecord> {
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
            val createdOrganization = gson
                .fromJson(createOrganizationCall.response.content, Organization::class.java)

            val adminUserId = mockStore.organizationIdMap[createdOrganization.id]!!.adminUser
            val createdUser = usersFrom(mockStore.userIdMap[adminUserId]!!)

            // Create a Credential to make API requests for the test
            // TODO: Replace this call with `/login` API once it's implemented so that
            //  the createCredential can be called with the returned JWT token
            val createdCredentials = MockCredentialsStore(mockStore).createCredential(createdUser.hrn)

            return Triple(createdOrganization, createdUser, createdCredentials)
        }
    }
}
