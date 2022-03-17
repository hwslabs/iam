package com.hypto.iam.server.helpers

import com.google.gson.Gson
import com.hypto.iam.server.db.Tables.CREDENTIALS
import com.hypto.iam.server.db.Tables.RESOURCES
import com.hypto.iam.server.db.Tables.USERS
import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.koin.test.inject
import org.koin.test.junit5.AutoCloseKoinTest

object DataSetupHelper : AutoCloseKoinTest() {
    private val gson = Gson()
    private const val rootToken = "hypto-root-secret-key"

    private val organizationRepo: OrganizationRepo by inject()
    private val userRepo: UserRepo by inject()
    private val policyRepo: PoliciesRepo by inject()
    private val userPolicyRepo: UserPoliciesRepo by inject()
    private val resourceRepo: ResourceRepo by inject()
    private val credentialRepo: CredentialsRepo by inject()

    fun createOrganizationUserCredential(
        engine: TestApplicationEngine
    ): Pair<CreateOrganizationResponse, AdminUser> {
        with(engine) {
            // Create organization
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()

            val adminUser = AdminUser(
                username = userName,
                passwordHash = "#123",
                email = "testAdminUser@example.com",
                phone = ""
            )

            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            adminUser
                        )
                    )
                )
            }

            val createdOrganizationResponse = gson
                .fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            return Pair(createdOrganizationResponse, adminUser)
        }
    }

    fun deleteOrganization(orgId: String, engine: TestApplicationEngine) {
        with(engine) {
            // TODO: Optimize the queries to do it one query
            policyRepo.fetchByOrganizationId(orgId).forEach { policies ->
                userPolicyRepo.fetch(USER_POLICIES.POLICY_HRN, policies.hrn).forEach {
                    userPolicyRepo.delete(it)
                }
                policyRepo.delete(policies)
            }
            resourceRepo.fetch(RESOURCES.ORGANIZATION_ID, orgId).forEach { resource ->
                resourceRepo.delete(resource)
            }
            userRepo.fetch(USERS.ORGANIZATION_ID, orgId).forEach { users ->
                credentialRepo.fetch(CREDENTIALS.USER_HRN, users.hrn).forEach {
                    credentialRepo.delete(it)
                }
                userRepo.delete(users)
            }
            organizationRepo.deleteById(orgId)
        }
    }
}
