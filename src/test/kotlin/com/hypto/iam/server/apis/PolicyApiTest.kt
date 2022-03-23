package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.PolicyPaginatedResponse
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.UpdatePolicyRequest
import com.hypto.iam.server.utils.IamResources
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PolicyApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Nested
    @DisplayName("Create policy API tests")
    inner class CreatePolicyTest {
        @Test
        fun `valid policy - success`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, _) = DataSetupHelper
                    .createOrganization(this)

                val createdOrganization = createdOrganizationResponse.organization!!
                val createdCredentials = createdOrganizationResponse.adminUserCredential!!

                val policyName = "test-policy"

                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
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
                        resource = IamResources.POLICY,
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

        @Test
        fun `policy name already in use`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)
                val createdOrganization = createOrganizationResponse.organization!!

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createOrganizationResponse.organization?.id}/policies"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                    )
                    setBody(gson.toJson(requestBody))
                }

                // Act
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    // Asset
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `policy without statements - failure`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                val policyName = "SamplePolicy"
                val requestBody = CreatePolicyRequest(policyName, listOf())

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `policy with too many policy statements - failure`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                val policyName = "SamplePolicy"
//                val policyStatements = mutableListOf<PolicyStatement>()

                val requestBody = CreatePolicyRequest(
                    policyName,
                    (0..50).map {
                        PolicyStatement("resource$it", "action$it", PolicyStatement.Effect.allow)
                    }
                )

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Get policy API tests")
    inner class GetPolicyTest {
        @Test
        fun `existing policy - success`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)
                val createdOrganization = createOrganizationResponse.organization!!

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createOrganizationResponse.organization?.id}/policies"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                    )
                    setBody(gson.toJson(requestBody))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.response.content, Policy::class.java)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/${createdPolicy.name}"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, Policy::class.java)
                    Assertions.assertEquals(createOrganizationResponse.organization?.id, responseBody.organizationId)

                    val expectedPolicyHrn = ResourceHrn(
                        organization = createOrganizationResponse.organization?.id!!,
                        resource = IamResources.POLICY,
                        account = null,
                        resourceInstance = policyName
                    )

                    Assertions.assertEquals(expectedPolicyHrn.toString(), responseBody.hrn)
                    Assertions.assertEquals(policyName, responseBody.name)
                    Assertions.assertEquals(1, responseBody.version)
                    Assertions.assertEquals(policyStatements, responseBody.statements)
                }
            }
        }

        @Test
        fun `non existing policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("List policies API tests")
    inner class ListPoliciesTest {
        @Test
        fun `existing policy - first page, more pages available`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)
                val createdOrganization = createOrganizationResponse.organization!!

                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

                // We create 2 policies in addition to 1 ADMIN_ROOT_POLICY.
                // So, list API must return 3 policies in total.
                (1..2).forEach {
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies"
                    ) {
                        val requestBody = CreatePolicyRequest("SamplePolicy$it", policyStatements)
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                        setBody(gson.toJson(requestBody))
                    }
                }

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies?pageSize=2"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert - first page
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)
                    Assertions.assertNotNull(responseBody.nextToken)
                    Assertions.assertEquals(2, responseBody.data?.size)
                    Assertions.assertEquals(2, responseBody.context?.pageSize)
                    Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody.context?.sortOrder)

                    // Assert - Last page

                    with(
                        handleRequest(
                            HttpMethod.Get,
                            "/organizations/${createOrganizationResponse.organization?.id}/policies?" +
                                "pageSize=3&nextToken=${responseBody.nextToken}"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(
                                HttpHeaders.Authorization,
                                "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                            )
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.OK, response.status())
                        Assertions.assertEquals(
                            ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            response.contentType()
                        )

                        val responseBody2 = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)

                        Assertions.assertNotNull(responseBody2.nextToken)
                        Assertions.assertEquals(1, responseBody2.data?.size)
                        Assertions.assertEquals(2, responseBody2.context?.pageSize)
                        Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody2.context?.sortOrder)

                        // Assert - Empty page

                        with(
                            handleRequest(
                                HttpMethod.Get,
                                "/organizations/${createOrganizationResponse.organization?.id}/policies?" +
                                    "pageSize=2&nextToken=${responseBody2.nextToken}"
                            ) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(
                                    HttpHeaders.Authorization,
                                    "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                                )
                            }
                        ) {
                            Assertions.assertEquals(HttpStatusCode.OK, response.status())
                            Assertions.assertEquals(
                                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                                response.contentType()
                            )

                            val responseBody3 = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)

                            Assertions.assertNull(responseBody3.nextToken)
                            Assertions.assertEquals(0, responseBody3.data?.size)
                            Assertions.assertEquals(2, responseBody3.context?.pageSize)
                            Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody3.context?.sortOrder)
                        }
                    }
                }
            }
        }

        @Test
        fun `no policies available (Just root policy)`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies?pageSize=2"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert - first page
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)
                    Assertions.assertNotNull(responseBody.nextToken)
                    Assertions.assertEquals(1, responseBody.data?.size)
                    Assertions.assertEquals(2, responseBody.context?.pageSize)
                    Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody.context?.sortOrder)

                    // Assert - Empty page

                    with(
                        handleRequest(
                            HttpMethod.Get,
                            "/organizations/${createOrganizationResponse.organization?.id}/policies?" +
                                "pageSize=2&nextToken=${responseBody.nextToken}"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(
                                HttpHeaders.Authorization,
                                "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                            )
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.OK, response.status())
                        Assertions.assertEquals(
                            ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            response.contentType()
                        )

                        val responseBody2 = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)

                        Assertions.assertNull(responseBody2.nextToken)
                        Assertions.assertEquals(0, responseBody2.data?.size)
                        Assertions.assertEquals(2, responseBody2.context?.pageSize)
                        Assertions.assertEquals(PaginationOptions.SortOrder.asc, responseBody2.context?.sortOrder)
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Update policy API tests")
    inner class UpdatePoliciesTest {
        @Test
        fun `update policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                // Create a policy
                val createdOrganization = createdOrganizationResponse.organization!!

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/policies"
                    ) {
                        val policyStatements = listOf(
                            PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow)
                        )
                        val requestBody = CreatePolicyRequest(policyName, policyStatements)
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                        )
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                }

                // Act

                // Update the created policy

                val updatePolicyRequest = UpdatePolicyRequest(
                    listOf(
                        PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow)
                    )
                )
                with(
                    handleRequest(
                        HttpMethod.Patch,
                        "/organizations/${createdOrganization.id}/policies/$policyName"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                        )

                        setBody(gson.toJson(updatePolicyRequest))
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val results = gson.fromJson(response.content, Policy::class.java)
                    Assertions.assertEquals(results.statements, updatePolicyRequest.statements)
                }
            }
        }

        @Test
        fun `update non-existent policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                // Act

                // Update the created policy
                val createdOrganization = createdOrganizationResponse.organization!!
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )

                val updatePolicyRequest = UpdatePolicyRequest(
                    listOf(
                        PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow)
                    )
                )
                with(
                    handleRequest(
                        HttpMethod.Patch,
                        "/organizations/${createdOrganization.id}/policies/non_existent_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                        )

                        setBody(gson.toJson(updatePolicyRequest))
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Delete policy API tests")
    inner class DeletePolicyTest {
        @Test
        fun `delete existing policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                val createdOrganization = createOrganizationResponse.organization!!

                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                    createdOrganization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

                val requestBody = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createOrganizationResponse.organization?.id}/policies"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                    )
                    setBody(gson.toJson(requestBody))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.response.content, Policy::class.java)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/${createdPolicy.name}"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `delete non existent policy`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createOrganizationResponse, _) = DataSetupHelper.createOrganization(this)

                // Act
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existent+policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Get policies attached to user API test")
    inner class GetPoliciesByUserTest {
        @Test
        fun `get policies of a user`() {
            withTestApplication(Application::handleRequest) {
                // Arrange
                val (createdOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!

                (1..2).forEach {
                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/organizations/${createdOrganization.id}/policies"
                        ) {
                            val resourceName = "resource"
//                            val resourceHrn = ResourceHrn(createdOrganization.id, null, resourceName, null).toString()
//                            val actionHrn = ActionHrn(createdOrganization.id, null, resourceName, "action").toString()
                            val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
                                createdOrganization.id,
                                null,
                                resourceName,
                                "action"
                            )
                            val policyStatements =
                                listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

                            val requestBody = CreatePolicyRequest("SamplePolicy$it", policyStatements)
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(
                                HttpHeaders.Authorization,
                                "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                            )
                            setBody(gson.toJson(requestBody))
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.Created, response.status())
                        val responseBody = gson.fromJson(response.content, Policy::class.java)

                        // Associate policy to a user
                        with(
                            handleRequest(
                                HttpMethod.Put,
                                "/organizations/${createdOrganization.id}/users/" +
                                    "${createdUser.username}/attach_policies"
                            ) {
                                val createAssociationRequest = PolicyAssociationRequest(listOf(responseBody.hrn))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(
                                    HttpHeaders.Authorization,
                                    "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                                )
                                setBody(gson.toJson(createAssociationRequest))
                            }
                        ) {
                            Assertions.assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }

                // Act
                val pageSize = 2
                var nextToken: String? = null

                // First page
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/${createdUser.username}" +
                            "/policies?pageSize=$pageSize"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val results = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)
                    Assertions.assertEquals(pageSize, results.data?.size)
                    Assertions.assertNotNull(results.nextToken)

                    nextToken = results.nextToken
                }

                // Second and last page with results
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/${createdUser.username}" +
                            "/policies?pageSize=$pageSize${nextToken?.let { "&nextToken=$it" }}"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val results = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)
                    nextToken = results.nextToken

                    Assertions.assertEquals(1, results.data?.size)
                    Assertions.assertNotNull(results.nextToken)
                }

                // Empty page
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/${createdUser.username}" +
                            "/policies?pageSize=$pageSize${nextToken?.let { "&nextToken=$it" }}"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganizationResponse.adminUserCredential?.secret}"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    val results = gson.fromJson(response.content, PolicyPaginatedResponse::class.java)
                    nextToken = results.nextToken

                    Assertions.assertEquals(0, results.data?.size)
                    Assertions.assertNull(results.nextToken)
                }
            }
        }
    }
}
