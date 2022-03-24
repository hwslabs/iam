@file:Suppress("LongParameterList")
package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.toUserStatus
import com.hypto.iam.server.idp.IdentityGroup
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.idp.NextToken
import com.hypto.iam.server.idp.PasswordCredentials
import com.hypto.iam.server.idp.RequestContext
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UsersServiceImpl : KoinComponent, UsersService {
    private val organizationRepo: OrganizationRepo by inject()
    private val identityProvider: IdentityProvider by inject()
    private val gson: Gson by inject()

    override suspend fun createUser(
        organizationId: String,
        credentials: PasswordCredentials,
        createdBy: String?
    ): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to create a user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, credentials.userName)
        val user = identityProvider.createUser(
            RequestContext(organizationId = organizationId, requestedPrincipal = createdBy ?: "unknown user"),
            identityGroup, credentials
        )
        return getUser(userHrn, user)
    }

    override suspend fun getUser(organizationId: String, userName: String): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val user = identityProvider.getUser(identityGroup, userName)
        return getUser(userHrn, user)
    }

    override suspend fun listUsers(
        organizationId: String,
        nextToken: String?,
        pageSize: Int?
    ): Triple<List<User>, NextToken?, PaginationOptions> {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val (users, nextToken) = identityProvider.listUsers(identityGroup = identityGroup,
            pageToken = nextToken, limit = pageSize)
        val pageContext = PaginationOptions(pageSize)
        val externalUserTypeUsers = users.map { user ->
            val userHrn = ResourceHrn(organizationId, "", IamResources.USER, user.username)
            getUser(userHrn, user)
        }.toList()
        return Triple(externalUserTypeUsers, nextToken, pageContext)
    }

    private fun getUser(
        userHrn: ResourceHrn,
        user: com.hypto.iam.server.idp.User
    ) = User(
        hrn = userHrn.toString(),
        username = user.username,
        organizationId = userHrn.organization,
        email = user.email,
        phone = user.phoneNumber,
        status = if (user.isEnabled) User.Status.enabled else User.Status.disabled,
        loginAccess = true, createdBy = user.createdBy
    )

    override suspend fun updateUser(
        organizationId: String,
        userName: String,
        email: String,
        phone: String,
        status: UpdateUserRequest.Status?
    ): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val userStatus = status?.toUserStatus()
        val user = identityProvider.updateUser(identityGroup = identityGroup,
            userName = userName, email = email, phone = phone, status = userStatus)
        return getUser(userHrn, user)
    }

    override suspend fun deleteUser(organizationId: String, userName: String): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to delete user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.deleteUser(identityGroup, userName)
        return BaseSuccessResponse(true)
    }
}

/**
 * Service which holds logic related to User operations
 */
interface UsersService {
    suspend fun createUser(organizationId: String, credentials: PasswordCredentials, createdBy: String?): User
    suspend fun getUser(organizationId: String, userName: String): User
    suspend fun listUsers(organizationId: String, nextToken: String?, pageSize: Int?):
        Triple<List<User>, NextToken?, PaginationOptions>
    suspend fun updateUser(
        organizationId: String,
        userName: String,
        email: String,
        phone: String,
        status: UpdateUserRequest.Status? = null
    ): User
    suspend fun deleteUser(organizationId: String, userName: String): BaseSuccessResponse
}
