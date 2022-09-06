package com.hypto.iam.server.helpers

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.Tables.ACTIONS
import com.hypto.iam.server.db.Tables.RESOURCES
import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.Resource
import com.hypto.iam.server.models.RootUser
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IdGenerator
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import kotlinx.coroutines.runBlocking
import org.koin.test.inject
import org.koin.test.junit5.AutoCloseKoinTest

object DataSetupHelper : AutoCloseKoinTest() {
    private val gson: Gson by inject()
    private val appConfig: AppConfig by inject()
    private val rootToken = appConfig.app.secretKey

    private val organizationRepo: OrganizationRepo by inject()
    private val policyRepo: PoliciesRepo by inject()
    private val userPolicyRepo: UserPoliciesRepo by inject()
    private val resourceRepo: ResourceRepo by inject()
    private val actionRepo: ActionRepo by inject()

    fun createOrganization(
        engine: TestApplicationEngine,
        preferredUsername: String = "user" + IdGenerator.randomId()
    ): Pair<CreateOrganizationResponse, RootUser> {
        with(engine) {
            // Create organization
            val orgName = "test-org" + IdGenerator.randomId()
            val name = "lorem ipsum"
            val testEmail = "test-email" + IdGenerator.randomId() + "@iam.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val rootUser = RootUser(
                preferredUsername = preferredUsername,
                name = name,
                passwordHash = testPassword,
                email = testEmail,
                phone = testPhone,
                verified = true
            )

            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            rootUser
                        )
                    )
                )
            }

            val createdOrganizationResponse = gson
                .fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            return Pair(createdOrganizationResponse, rootUser)
        }
    }

    fun createCredential(
        orgId: String,
        userName: String,
        jwtToken: String,
        engine: TestApplicationEngine
    ): Credential {
        with(engine) {
            val createCredentialCall = handleRequest(
                HttpMethod.Post,
                "/organizations/$orgId/users/$userName/credentials"
            ) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $jwtToken")
                setBody(gson.toJson(CreateCredentialRequest()))
            }

            return gson
                .fromJson(createCredentialCall.response.content, Credential::class.java)
        }
    }

    fun createUser(
        orgId: String,
        bearerToken: String,
        createUserRequest: CreateUserRequest,
        engine: TestApplicationEngine
    ): Pair<User, Credential> {
        with(engine) {
            val createUserCall = handleRequest(HttpMethod.Post, "/organizations/$orgId/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $bearerToken")
                setBody(gson.toJson(createUserRequest))
            }
            val createdUser = gson
                .fromJson(createUserCall.response.content, User::class.java)
            val credential =
                createCredential(orgId, createdUser.username, bearerToken, engine)

            return Pair(createdUser, credential)
        }
    }

    fun createAndAttachPolicy(
        orgId: String,
        username: String?,
        bearerToken: String,
        policyName: String,
        accountId: String?,
        resourceName: String,
        actionName: String,
        resourceInstance: String,
        engine: TestApplicationEngine,
        effect: PolicyStatement.Effect = PolicyStatement.Effect.allow
    ): Policy {
        with(engine) {
            val (resourceHrn, actionHrn) = createResourceActionHrn(
                orgId,
                accountId,
                resourceName,
                actionName,
                resourceInstance
            )
            val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, effect))
            val requestBody = CreatePolicyRequest(policyName, policyStatements)

            val createPolicyCall = handleRequest(
                HttpMethod.Post,
                "/organizations/$orgId/policies"
            ) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $bearerToken")
                setBody(gson.toJson(requestBody))
            }

            val policy = gson.fromJson(createPolicyCall.response.content, Policy::class.java)

            if (username != null) {
                handleRequest(
                    HttpMethod.Patch,
                    "/organizations/$orgId/users/" +
                        "$username/attach_policies"
                ) {
                    val createAssociationRequest = PolicyAssociationRequest(listOf(policy.hrn))
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer $bearerToken"
                    )
                    setBody(gson.toJson(createAssociationRequest))
                }
            }
            return policy
        }
    }

    fun createResource(
        orgId: String,
        jwtToken: String,
        engine: TestApplicationEngine,
        resourceName: String? = null
    ): Resource {
        with(engine) {
            val name = resourceName ?: ("test-resource" + IdGenerator.randomId())

            val createResourceCall = handleRequest(HttpMethod.Post, "/organizations/$orgId/resources") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $jwtToken")
                setBody(gson.toJson(CreateResourceRequest(name = name)))
            }

            return gson
                .fromJson(createResourceCall.response.content, Resource::class.java)
        }
    }

    fun createAction(
        orgId: String,
        resource: Resource? = null,
        jwtToken: String,
        engine: TestApplicationEngine
    ): Pair<Action, Resource> {
        with(engine) {
            val createdResource = resource ?: createResource(orgId, jwtToken, engine)
            val actionName = "test-action" + IdGenerator.randomId()

            val createActionCall =
                handleRequest(HttpMethod.Post, "/organizations/$orgId/resources/${createdResource.name}/actions") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $jwtToken")
                    setBody(gson.toJson(CreateActionRequest(name = actionName)))
                }

            val createdAction = gson
                .fromJson(createActionCall.response.content, Action::class.java)

            return Pair(createdAction, createdResource)
        }
    }

    fun createResourceActionHrn(
        orgId: String,
        accountId: String?,
        resourceName: String,
        actionName: String,
        resourceInstance: String? = null
    ): Pair<String, String> {
        val resourceHrn = ResourceHrn(orgId, accountId, resourceName, resourceInstance).toString()
        val actionHrn = ActionHrn(orgId, accountId, resourceName, actionName).toString()
        return Pair(resourceHrn, actionHrn)
    }

    // This function is used for cleaning up all the data created during the test for the organization
    fun deleteOrganization(orgId: String, engine: TestApplicationEngine) {
        runBlocking {
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
                organizationRepo.deleteById(orgId)
            }
        }
    }
}
