package com.hypto.iam.server.helpers

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.Tables.ACTIONS
import com.hypto.iam.server.db.Tables.PRINCIPAL_POLICIES
import com.hypto.iam.server.db.Tables.RESOURCES
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.PrincipalPoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.CreateUserResponse
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
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.runBlocking
import org.koin.test.inject
import org.koin.test.junit5.AutoCloseKoinTest

object DataSetupHelperV2 : AutoCloseKoinTest() {
    private val gson: Gson by inject()
    private val appConfig: AppConfig by inject()
    private val rootToken = Constants.SECRET_PREFIX + appConfig.app.secretKey

    private val organizationRepo: OrganizationRepo by inject()
    private val policyRepo: PoliciesRepo by inject()
    private val principalPoliciesRepo: PrincipalPoliciesRepo by inject()
    private val resourceRepo: ResourceRepo by inject()
    private val actionRepo: ActionRepo by inject()

    suspend fun ApplicationTestBuilder.createOrganization(
        preferredUsername: String = "user" + IdGenerator.randomId()
    ): Pair<CreateOrganizationResponse, RootUser> {
        // Create organization
        val orgName = "test-org" + IdGenerator.randomId()
        val name = "lorem ipsum"
        val testEmail = "test-email" + IdGenerator.randomId() + "@iam.in"
        val testPhone = "+919626012778"
        val testPassword = "testPassword@Hash1"

        val rootUser = RootUser(
            preferredUsername = preferredUsername,
            name = name,
            password = testPassword,
            email = testEmail,
            phone = testPhone
        )

        val createOrganizationCall = client.post("/organizations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header("X-Api-Key", rootToken)
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
            .fromJson(createOrganizationCall.bodyAsText(), CreateOrganizationResponse::class.java)

        return Pair(createdOrganizationResponse, rootUser.copy(email = rootUser.email.lowercase()))
    }

    suspend fun ApplicationTestBuilder.createCredential(
        orgId: String,
        userName: String,
        jwtToken: String,
    ): Credential {
        val createCredentialCall = client.post(
            "/organizations/$orgId/users/$userName/credentials"
        ) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
            setBody(gson.toJson(CreateCredentialRequest()))
        }

        return gson
            .fromJson(createCredentialCall.bodyAsText(), Credential::class.java)
    }

    suspend fun ApplicationTestBuilder.createUser(
        orgId: String,
        bearerToken: String,
        createUserRequest: CreateUserRequest,
    ): Pair<User, Credential> {
        val createUserCall = client.post("/organizations/$orgId/users") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $bearerToken")
            setBody(gson.toJson(createUserRequest))
        }
        val createdUser = gson
            .fromJson(createUserCall.bodyAsText(), CreateUserResponse::class.java)
        val credential =
            createCredential(orgId, createdUser.user.username, bearerToken)

        return Pair(createdUser.user, credential)
    }

    suspend fun ApplicationTestBuilder.createAndAttachPolicy(
        orgId: String,
        username: String?,
        bearerToken: String,
        policyName: String,
        accountId: String?,
        resourceName: String,
        actionName: String,
        resourceInstance: String,
        effect: PolicyStatement.Effect = PolicyStatement.Effect.allow
    ): Policy {
        val (resourceHrn, actionHrn) = createResourceActionHrn(
            orgId,
            accountId,
            resourceName,
            actionName,
            resourceInstance
        )
        val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, effect))
        val requestBody = CreatePolicyRequest(policyName, policyStatements)

        val createPolicyCall = client.post(
            "/organizations/$orgId/policies"
        ) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $bearerToken")
            setBody(gson.toJson(requestBody))
        }

        val policy = gson.fromJson(createPolicyCall.bodyAsText(), Policy::class.java)

        if (username != null) {
            client.patch(
                "/organizations/$orgId/users/" +
                    "$username/attach_policies"
            ) {
                val createAssociationRequest = PolicyAssociationRequest(listOf(policy.hrn))
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(
                    HttpHeaders.Authorization,
                    "Bearer $bearerToken"
                )
                setBody(gson.toJson(createAssociationRequest))
            }
        }
        return policy
    }

    suspend fun ApplicationTestBuilder.createResource(
        orgId: String,
        jwtToken: String,
        resourceName: String? = null
    ): Resource {
        val name = resourceName ?: ("test-resource" + IdGenerator.randomId())

        val createResourceCall = client.post("/organizations/$orgId/resources") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
            setBody(gson.toJson(CreateResourceRequest(name = name)))
        }

        return gson
            .fromJson(createResourceCall.bodyAsText(), Resource::class.java)
    }

    suspend fun ApplicationTestBuilder.createAction(
        orgId: String,
        resource: Resource? = null,
        jwtToken: String,
    ): Pair<Action, Resource> {
        val createdResource = resource ?: createResource(orgId, jwtToken)
        val actionName = "test-action" + IdGenerator.randomId()

        val createActionCall =
            client.post("/organizations/$orgId/resources/${createdResource.name}/actions") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
                setBody(gson.toJson(CreateActionRequest(name = actionName)))
            }

        val createdAction = gson
            .fromJson(createActionCall.bodyAsText(), Action::class.java)

        return Pair(createdAction, createdResource)
    }

    fun ApplicationTestBuilder.createResourceActionHrn(
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
    fun ApplicationTestBuilder.deleteOrganization(orgId: String) {
        runBlocking {
            // TODO: Optimize the queries to do it one query (CASCADE DELETE)
            policyRepo.fetchByOrganizationId(orgId).forEach { policies ->
                principalPoliciesRepo.fetch(PRINCIPAL_POLICIES.POLICY_HRN, policies.hrn).forEach {
                    principalPoliciesRepo.delete(it)
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
