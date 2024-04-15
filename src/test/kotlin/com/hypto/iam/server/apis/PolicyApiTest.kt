package com.hypto.iam.server.apis

import com.hypto.iam.server.Constants
import com.hypto.iam.server.db.repositories.PolicyTemplatesRepo
import com.hypto.iam.server.db.tables.records.PolicyTemplatesRecord
import com.hypto.iam.server.helpers.BaseSingleAppTest
import com.hypto.iam.server.helpers.DataSetupHelper.deleteOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV3.createAndAttachPolicy
import com.hypto.iam.server.helpers.DataSetupHelperV3.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV3.createResourceActionHrn
import com.hypto.iam.server.helpers.DataSetupHelperV3.createUser
import com.hypto.iam.server.helpers.DataSetupHelperV3.deleteOrganization
import com.hypto.iam.server.models.CreatePolicyFromTemplateRequest
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.PolicyPaginatedResponse
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.UpdatePolicyRequest
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.IdGenerator
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.utils.policy.PolicyBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.test.dispatcher.testSuspend
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.test.inject
import java.time.LocalDateTime

internal class PolicyApiTest : BaseSingleAppTest() {
    @Nested
    @DisplayName("Create policy API tests")
    inner class CreatePolicyTest {
        @Test
        fun `valid policy - success`() {
            testSuspend {
                val (createdOrganizationResponse, _) = testApp.createOrganization()

                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken

                val policyName = "test-policy"

                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val response =
                    testApp.client.post(
                        "/organizations/${createdOrganization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.Created, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                Assertions.assertEquals(
                    createdOrganization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER],
                )

                val responseBody = gson.fromJson(response.bodyAsText(), Policy::class.java)
                Assertions.assertEquals(createdOrganization.id, responseBody.organizationId)

                val expectedPolicyHrn =
                    ResourceHrn(
                        organization = createdOrganization.id,
                        resource = IamResources.POLICY,
                        subOrganization = null,
                        resourceInstance = policyName,
                    )

                Assertions.assertEquals(expectedPolicyHrn.toString(), responseBody.hrn)
                Assertions.assertEquals(policyName, responseBody.name)
                Assertions.assertEquals(1, responseBody.version)
                Assertions.assertEquals(policyStatements, responseBody.statements)

                testApp.deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `policy name already in use`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()
                val createdOrganization = createOrganizationResponse.organization

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                testApp.client.post(
                    "/organizations/${createOrganizationResponse.organization.id}/policies",
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createOrganizationResponse.rootUserToken}",
                    )
                    setBody(gson.toJson(requestBody))
                }

                // Act
                val response =
                    testApp.client.post(
                        "/organizations/${createOrganizationResponse.organization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }
                // Asset
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }

        @Test
        fun `policy without statements - failure`() {
            testSuspend {
                val (createOrganizationResponse, _) = testApp.createOrganization()

                val policyName = "SamplePolicy"
                val requestBody = CreatePolicyRequest(policyName, listOf())

                val response =
                    testApp.client.post(
                        "/organizations/${createOrganizationResponse.organization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }

        @Test
        fun `policy with too many policy statements - failure`() {
            testSuspend {
                val (createOrganizationResponse, _) = testApp.createOrganization()

                val policyName = "SamplePolicy"
//                val policyStatements = mutableListOf<PolicyStatement>()

                val requestBody =
                    CreatePolicyRequest(
                        policyName,
                        (0..50).map {
                            PolicyStatement("resource$it", "action$it", PolicyStatement.Effect.allow)
                        },
                    )

                val response =
                    testApp.client.post(
                        "/organizations/${createOrganizationResponse.organization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }

        @Test
        fun `create policy with different org in principal - failure`() {
            testSuspend {
                val (createdOrganizationResponse, _) = testApp.createOrganization()

                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken

                val policyName = "testPolicy"

                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        "randomId",
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val response =
                    testApp.client.post(
                        "/organizations/${createdOrganization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                testApp.deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `create policy from template - success`() {
            testSuspend {
                val (createdOrganizationResponse, _) = testApp.createOrganization()

                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken

                val policyName = "test-policy"
                val policyTemplateName = "test-policy-template"

                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val policyHrn = ResourceHrn(createdOrganization.id, "", IamResources.POLICY, policyName)

                val policyStr = PolicyBuilder(policyHrn).withStatement(policyStatements[0]).build()

                val policyTemplatesRepo: PolicyTemplatesRepo by inject()
                val policyTempRecord = PolicyTemplatesRecord()
                policyTempRecord.apply {
                    name = policyTemplateName
                    status = "ACTIVE"
                    isRootPolicy = false
                    onCreateOrg = false
                    statements = policyStr // policyStatements.toString()
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                    description = ""
                    requiredVariables = arrayOf<String>()
                }

                policyTemplatesRepo.store(policyTempRecord)

                val requestBody = CreatePolicyFromTemplateRequest(policyName, policyTemplateName)

                val response =
                    testApp.client.post(
                        "/organizations/${createdOrganization.id}/policy_from_template",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.Created, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                Assertions.assertEquals(
                    createdOrganization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER],
                )

                val responseBody = gson.fromJson(response.bodyAsText(), Policy::class.java)
                Assertions.assertEquals(createdOrganization.id, responseBody.organizationId)

                val expectedPolicyHrn =
                    ResourceHrn(
                        organization = createdOrganization.id,
                        resource = IamResources.POLICY,
                        subOrganization = null,
                        resourceInstance = policyName,
                    )

                Assertions.assertEquals(expectedPolicyHrn.toString(), responseBody.hrn)
                Assertions.assertEquals(policyName, responseBody.name)
                Assertions.assertEquals(1, responseBody.version)
                Assertions.assertEquals(policyStatements, responseBody.statements)

                // cleanup
                policyTempRecord.delete()
                testApp.deleteOrganization(createdOrganization.id)
            }
        }
    }

    @Nested
    @DisplayName("Get policy API tests")
    inner class GetPolicyTest {
        @Test
        fun `existing policy - success`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()
                val createdOrganization = createOrganizationResponse.organization

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall =
                    testApp.client.post(
                        "/organizations/${createOrganizationResponse.organization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }

                val createdPolicy = gson.fromJson(createPolicyCall.bodyAsText(), Policy::class.java)

                // Act
                val response =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies/${createdPolicy.name}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                val responseBody = gson.fromJson(response.bodyAsText(), Policy::class.java)
                Assertions.assertEquals(createOrganizationResponse.organization.id, responseBody.organizationId)

                val expectedPolicyHrn =
                    ResourceHrn(
                        organization = createOrganizationResponse.organization.id,
                        resource = IamResources.POLICY,
                        subOrganization = null,
                        resourceInstance = policyName,
                    )

                Assertions.assertEquals(expectedPolicyHrn.toString(), responseBody.hrn)
                Assertions.assertEquals(policyName, responseBody.name)
                Assertions.assertEquals(1, responseBody.version)
                Assertions.assertEquals(policyStatements, responseBody.statements)
            }
        }

        @Test
        fun `non existing policy`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()

                // Act
                val response =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }
    }

    @Nested
    @DisplayName("List policies API tests")
    inner class ListPoliciesTest {
        @Test
        fun `existing policy - first page, more pages available`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()
                val createdOrganization = createOrganizationResponse.organization

                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

                // We create 2 policies in addition to 1 ADMIN_ROOT_POLICY.
                // So, list API must return 3 policies in total.
                (1..2).forEach {
                    testApp.client.post(
                        "/organizations/${createOrganizationResponse.organization.id}/policies",
                    ) {
                        val requestBody = CreatePolicyRequest("SamplePolicy$it", policyStatements)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }
                }

                // Act
                val actResponse =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies?pageSize=2",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert - first page
                Assertions.assertEquals(HttpStatusCode.OK, actResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    actResponse.contentType(),
                )

                val responseBody = gson.fromJson(actResponse.bodyAsText(), PolicyPaginatedResponse::class.java)
                Assertions.assertNotNull(responseBody.nextToken)
                Assertions.assertEquals(2, responseBody.data?.size)
                Assertions.assertEquals(2, responseBody.context?.pageSize)
                Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody.context?.sortOrder)

                // Assert - Last page

                val lastPageResponse =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies?" +
                            "pageSize=3&nextToken=${responseBody.nextToken}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                Assertions.assertEquals(HttpStatusCode.OK, lastPageResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    lastPageResponse.contentType(),
                )

                val responseBody2 =
                    gson.fromJson(
                        lastPageResponse.bodyAsText(),
                        PolicyPaginatedResponse::class.java,
                    )

                Assertions.assertNotNull(responseBody2.nextToken)
                Assertions.assertEquals(1, responseBody2.data?.size)
                Assertions.assertEquals(2, responseBody2.context?.pageSize)
                Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody2.context?.sortOrder)

                // Assert - Empty page

                val emptyPageResponse =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies?" +
                            "pageSize=2&nextToken=${responseBody2.nextToken}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                Assertions.assertEquals(HttpStatusCode.OK, emptyPageResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    emptyPageResponse.contentType(),
                )

                val responseBody3 =
                    gson.fromJson(
                        emptyPageResponse.bodyAsText(),
                        PolicyPaginatedResponse::class.java,
                    )

                Assertions.assertNull(responseBody3.nextToken)
                Assertions.assertEquals(0, responseBody3.data?.size)
                Assertions.assertEquals(2, responseBody3.context?.pageSize)
                Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody3.context?.sortOrder)
            }
        }

        @Test
        fun `no policies available (Just root policy)`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()

                // Act
                val actResponse =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies?pageSize=2",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert - first page
                Assertions.assertEquals(HttpStatusCode.OK, actResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    actResponse.contentType(),
                )

                val responseBody = gson.fromJson(actResponse.bodyAsText(), PolicyPaginatedResponse::class.java)
                Assertions.assertNotNull(responseBody.nextToken)
                Assertions.assertEquals(1, responseBody.data?.size)
                Assertions.assertEquals(2, responseBody.context?.pageSize)
                Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody.context?.sortOrder)

                // Assert - Empty page

                val emptyPageResponse =
                    testApp.client.get(
                        "/organizations/${createOrganizationResponse.organization.id}/policies?" +
                            "pageSize=2&nextToken=${responseBody.nextToken}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                Assertions.assertEquals(HttpStatusCode.OK, emptyPageResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    emptyPageResponse.contentType(),
                )

                val responseBody2 =
                    gson.fromJson(
                        emptyPageResponse.bodyAsText(),
                        PolicyPaginatedResponse::class.java,
                    )

                Assertions.assertNull(responseBody2.nextToken)
                Assertions.assertEquals(0, responseBody2.data?.size)
                Assertions.assertEquals(2, responseBody2.context?.pageSize)
                Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody2.context?.sortOrder)
            }
        }
    }

    @Nested
    @DisplayName("Update policy API tests")
    inner class UpdatePoliciesTest {
        @Test
        fun `update policy`() {
            testSuspend {
                // Arrange
                val (createdOrganizationResponse, _) = testApp.createOrganization()

                // Create a policy
                val createdOrganization = createdOrganizationResponse.organization

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )

                val actResponse =
                    testApp.client.post(
                        "/organizations/${createdOrganization.id}/policies",
                    ) {
                        val policyStatements =
                            listOf(
                                PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow),
                            )
                        val requestBody = CreatePolicyRequest(policyName, policyStatements)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.Created, actResponse.status)

                // Act

                // Update the created policy

                val updatePolicyRequest =
                    UpdatePolicyRequest(
                        "new description",
                        listOf(
                            PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow),
                        ),
                    )
                val updatedCreatedPolicyResponse =
                    testApp.client.patch(
                        "/organizations/${createdOrganization.id}/policies/$policyName",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.rootUserToken}",
                        )

                        setBody(gson.toJson(updatePolicyRequest))
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.OK, updatedCreatedPolicyResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    updatedCreatedPolicyResponse.contentType(),
                )

                val results = gson.fromJson(updatedCreatedPolicyResponse.bodyAsText(), Policy::class.java)
                Assertions.assertEquals(results.description, updatePolicyRequest.description)
                Assertions.assertEquals(results.statements, updatePolicyRequest.statements)
            }
        }

        @Test
        fun `update non-existent policy`() {
            testSuspend {
                // Arrange
                val (createdOrganizationResponse, _) = testApp.createOrganization()

                // Act

                // Update the created policy
                val createdOrganization = createdOrganizationResponse.organization
                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )

                val updatePolicyRequest =
                    UpdatePolicyRequest(
                        null,
                        listOf(
                            PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow),
                        ),
                    )
                val response =
                    testApp.client.patch(
                        "/organizations/${createdOrganization.id}/policies/non_existent_policy",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.rootUserToken}",
                        )

                        setBody(gson.toJson(updatePolicyRequest))
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }
    }

    @Nested
    @DisplayName("Delete policy API tests")
    inner class DeletePolicyTest {
        @Test
        fun `delete existing policy`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()

                val createdOrganization = createOrganizationResponse.organization

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) =
                    testApp.createResourceActionHrn(
                        createdOrganization.id,
                        null,
                        resourceName,
                        "action",
                    )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall =
                    testApp.client.post(
                        "/organizations/${createOrganizationResponse.organization.id}/policies",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                        setBody(gson.toJson(requestBody))
                    }

                val createdPolicy = gson.fromJson(createPolicyCall.bodyAsText(), Policy::class.java)

                // Act
                val response =
                    testApp.client.delete(
                        "/organizations/${createOrganizationResponse.organization.id}/policies/${createdPolicy.name}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }

        @Test
        fun `delete non existent policy`() {
            testSuspend {
                // Arrange
                val (createOrganizationResponse, _) = testApp.createOrganization()

                // Act
                val response =
                    testApp.client.delete(
                        "/organizations/${createOrganizationResponse.organization.id}/policies/non_existent+policy",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
            }
        }
    }

    @Nested
    @DisplayName("Get policies attached to user API test")
    inner class GetPoliciesByUserTest {
        @Test
        fun `get policies of a user`() {
            testSuspend {
                // Arrange
                val (createdOrganizationResponse, createdUser) = testApp.createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val username = createdOrganizationResponse.organization.rootUser.username

                (1..2).forEach {
                    val response =
                        testApp.client.post(
                            "/organizations/${createdOrganization.id}/policies",
                        ) {
                            val resourceName = "resource"
                            val (resourceHrn, actionHrn) =
                                testApp.createResourceActionHrn(
                                    createdOrganization.id,
                                    null,
                                    resourceName,
                                    "action",
                                )
                            val policyStatements =
                                listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

                            val requestBody = CreatePolicyRequest("SamplePolicy$it", policyStatements)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(
                                HttpHeaders.Authorization,
                                "Bearer ${createdOrganizationResponse.rootUserToken}",
                            )
                            setBody(gson.toJson(requestBody))
                        }
                    Assertions.assertEquals(HttpStatusCode.Created, response.status)
                    val responseBody = gson.fromJson(response.bodyAsText(), Policy::class.java)

                    // Associate policy to a user
                    val associatedResponse =
                        testApp.client.patch(
                            "/organizations/${createdOrganization.id}/users/" +
                                "${createdOrganizationResponse.organization.rootUser.username}/attach_policies",
                        ) {
                            val createAssociationRequest = PolicyAssociationRequest(listOf(responseBody.hrn))
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(
                                HttpHeaders.Authorization,
                                "Bearer ${createdOrganizationResponse.rootUserToken}",
                            )
                            setBody(gson.toJson(createAssociationRequest))
                        }
                    Assertions.assertEquals(HttpStatusCode.OK, associatedResponse.status)
                }

                // Act
                val pageSize = 2
                var nextToken: String? = null

                // First page
                val firstResponse =
                    testApp.client.get(
                        "/organizations/${createdOrganization.id}/users/$username" +
                            "/policies?pageSize=$pageSize",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.OK, firstResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    firstResponse.contentType(),
                )

                val firstResult = gson.fromJson(firstResponse.bodyAsText(), PolicyPaginatedResponse::class.java)
                Assertions.assertEquals(pageSize, firstResult.data?.size)
                Assertions.assertNotNull(firstResult.nextToken)

                nextToken = firstResult.nextToken

                // Second and last page with results
                val secondAndLastResponse =
                    testApp.client.get(
                        "/organizations/${createdOrganization.id}/users/$username" +
                            "/policies?pageSize=$pageSize${nextToken?.let { "&nextToken=$it" }}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.OK, secondAndLastResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    secondAndLastResponse.contentType(),
                )

                val secondAndLastResult =
                    gson.fromJson(
                        secondAndLastResponse.bodyAsText(),
                        PolicyPaginatedResponse::class.java,
                    )
                nextToken = secondAndLastResult.nextToken

                Assertions.assertEquals(1, secondAndLastResult.data?.size)
                Assertions.assertNotNull(secondAndLastResult.nextToken)

                // Empty page
                val emptyPageResponse =
                    testApp.client.get(
                        "/organizations/${createdOrganization.id}/users/$username" +
                            "/policies?pageSize=$pageSize${nextToken?.let { "&nextToken=$it" }}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.rootUserToken}",
                        )
                    }
                // Assert
                Assertions.assertEquals(HttpStatusCode.OK, emptyPageResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    emptyPageResponse.contentType(),
                )

                val emptyPageResult =
                    gson.fromJson(
                        emptyPageResponse.bodyAsText(),
                        PolicyPaginatedResponse::class.java,
                    )
                nextToken = emptyPageResult.nextToken

                Assertions.assertEquals(0, emptyPageResult.data?.size)
                Assertions.assertNull(emptyPageResult.nextToken)
            }
        }

        @Test
        fun `user to list policies of other user without permission - failure`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken

                val createUser1Request =
                    CreateUserRequest(
                        preferredUsername = "testUserName1",
                        name = "lorem ipsum",
                        password = "testPassword@Hash1",
                        email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                        status = CreateUserRequest.Status.enabled,
                        phone = "+919999999999",
                        verified = true,
                        loginAccess = true,
                    )

                val (user1, _) =
                    testApp.createUser(
                        organization.id,
                        rootUserToken,
                        createUser1Request,
                    )

                val createUser2Request =
                    CreateUserRequest(
                        preferredUsername = "testUserName2",
                        name = "lorem ipsum",
                        password = "testPassword@Hash2",
                        email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                        status = CreateUserRequest.Status.enabled,
                        phone = "+919999999999",
                        verified = true,
                        loginAccess = true,
                    )
                val (user2, credential) =
                    testApp.createUser(
                        organization.id,
                        rootUserToken,
                        createUser2Request,
                    )
                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = user2.username,
                    bearerToken = rootUserToken,
                    policyName = "test-policy",
                    subOrgName = null,
                    resourceName = IamResources.USER,
                    actionName = "getUserPolicy",
                    resourceInstance = user2.username,
                )
                val response =
                    testApp.client.get(
                        "/organizations/${organization.id}/users/${user1.username}/policies",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${credential.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                Assertions.assertEquals(HttpStatusCode.Forbidden, response.status)
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `user to list policy of a user with permission - success`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken

                val createUser1Request =
                    CreateUserRequest(
                        preferredUsername = "testUserName1",
                        name = "lorem ipsum",
                        password = "testPassword@Hash1",
                        email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                        status = CreateUserRequest.Status.enabled,
                        phone = "+919999999999",
                        verified = true,
                        loginAccess = true,
                    )

                val (user1, _) =
                    testApp.createUser(
                        organization.id,
                        rootUserToken,
                        createUser1Request,
                    )
                val createUser2Request =
                    CreateUserRequest(
                        preferredUsername = "testUserName2",
                        name = "lorem ipsum",
                        password = "testPassword@Hash2",
                        email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                        status = CreateUserRequest.Status.enabled,
                        phone = "+919999999999",
                        verified = true,
                        loginAccess = true,
                    )
                val (user2, credential) =
                    testApp.createUser(
                        organization.id,
                        rootUserToken,
                        createUser2Request,
                    )
                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = user2.username,
                    bearerToken = rootUserToken,
                    policyName = "test-policy",
                    subOrgName = null,
                    resourceName = IamResources.USER,
                    actionName = "getUserPolicy",
                    resourceInstance = user1.username,
                )
                val response =
                    testApp.client.get(
                        "/organizations/${organization.id}/users/${user1.username}/policies",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${credential.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                val responseBody = gson.fromJson(response.bodyAsText(), PolicyPaginatedResponse::class.java)
                Assertions.assertEquals(0, responseBody.data!!.size)
                testApp.deleteOrganization(organization.id)
            }
        }
    }
}
