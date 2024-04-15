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
import com.hypto.iam.server.helpers.DataSetupHelperV2.toStringEscape
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.RootUser
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
    private val rootToken = Constants.SECRET_PREFIX + appConfig.app.secretKey

    private val organizationRepo: OrganizationRepo by inject()
    private val policyRepo: PoliciesRepo by inject()
    private val principalPoliciesRepo: PrincipalPoliciesRepo by inject()
    private val resourceRepo: ResourceRepo by inject()
    private val actionRepo: ActionRepo by inject()

    fun createOrganization(
        engine: TestApplicationEngine,
        preferredUsername: String = "user" + IdGenerator.randomId(),
    ): Pair<CreateOrganizationResponse, RootUser> {
        with(engine) {
            // Create organization
            val orgName = "test-org" + IdGenerator.randomId()
            val name = "lorem ipsum"
            val testEmail = "test-email" + IdGenerator.randomId() + "@iam.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val rootUser =
                RootUser(
                    preferredUsername = preferredUsername,
                    name = name,
                    password = testPassword,
                    email = testEmail,
                    phone = testPhone,
                )

            val createOrganizationCall =
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", rootToken)
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                rootUser,
                            ),
                        ),
                    )
                }

            val createdOrganizationResponse =
                gson
                    .fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            return Pair(createdOrganizationResponse, rootUser.copy(email = rootUser.email.lowercase()))
        }
    }

    fun createResourceActionHrn(
        orgId: String,
        accountId: String?,
        resourceName: String,
        actionName: String,
        resourceInstance: String? = null,
    ): Pair<String, String> {
        val resourceHrn = ResourceHrn(orgId, accountId, resourceName, resourceInstance).toString()
        val actionHrn = ActionHrn(orgId, accountId, resourceName, actionName).toStringEscape()
        return Pair(resourceHrn, actionHrn)
    }

    // This function is used for cleaning up all the data created during the test for the organization
    fun deleteOrganization(orgId: String) {
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
