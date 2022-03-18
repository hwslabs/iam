package com.hypto.iam.server.helpers

import com.google.gson.Gson
import com.hypto.iam.server.db.Tables.ACTIONS
import com.hypto.iam.server.db.Tables.CREDENTIALS
import com.hypto.iam.server.db.Tables.RESOURCES
import com.hypto.iam.server.db.Tables.USERS
import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.Resource
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
    private val actionRepo: ActionRepo by inject()

    fun createOrganization(
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

    fun createResource(
        orgId: String,
        userCredential: Credential,
        engine: TestApplicationEngine,
        resourceName: String? = null
    ): Resource {
        with(engine) {
            val name = resourceName ?: ("test-resource" + IdGenerator.randomId())

            val createResourceCall = handleRequest(HttpMethod.Post, "/organizations/$orgId/resources") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${userCredential.secret}")
                setBody(gson.toJson(CreateResourceRequest(name = name)))
            }

            return gson
                .fromJson(createResourceCall.response.content, Resource::class.java)
        }
    }

    fun createAction(
        orgId: String,
        resource: Resource? = null,
        userCredential: Credential,
        engine: TestApplicationEngine
    ): Pair<Action, Resource> {
        with(engine) {
            val createdResource = resource ?: createResource(orgId, userCredential, engine)
            val actionName = "test-action" + IdGenerator.randomId()

            val createActionCall =
                handleRequest(HttpMethod.Post, "/organizations/$orgId/resources/${createdResource.name}/actions") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${userCredential.secret}")
                    setBody(gson.toJson(CreateActionRequest(name = actionName)))
                }

            val createdAction = gson
                .fromJson(createActionCall.response.content, Action::class.java)

            return Pair(createdAction, createdResource)
        }
    }

    // This function is used for cleaning up all the data created during the test for the organization
    fun deleteOrganization(orgId: String, engine: TestApplicationEngine) {

        with(engine) {
            // TODO: Optimize the queries to do it one query (CASCADE DELETE)
            policyRepo.fetchByOrganizationId(orgId).forEach { policies ->
                userPolicyRepo.fetch(USER_POLICIES.POLICY_HRN, policies.hrn).forEach {
                    userPolicyRepo.delete(it)
                }
                policyRepo.delete(policies)
            }
            resourceRepo.fetch(RESOURCES.ORGANIZATION_ID, orgId).forEach { resource ->
                actionRepo.fetch(ACTIONS.RESOURCE_HRN, resource.hrn).forEach {
                    actionRepo.delete(it)
                }
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
