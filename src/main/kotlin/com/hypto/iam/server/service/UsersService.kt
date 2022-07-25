@file:Suppress("LongParameterList")

package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.extensions.toUserStatus
import com.hypto.iam.server.idp.IdentityGroup
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.idp.NextToken
import com.hypto.iam.server.idp.PasswordCredentials
import com.hypto.iam.server.idp.RequestContext
import com.hypto.iam.server.idp.UserAlreadyExistException
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.server.plugins.BadRequestException
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UsersServiceImpl : KoinComponent, UsersService {
    private val userRepo: UserRepo by inject()
    private val organizationRepo: OrganizationRepo by inject()
    private val identityProvider: IdentityProvider by inject()
    private val gson: Gson by inject()
    private val appConfig: AppConfig by inject()

    override suspend fun createUser(
        organizationId: String,
        credentials: PasswordCredentials,
        createdBy: String?,
        verified: Boolean
    ): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to create a user")

        if (UserRepo.existsByEmail(credentials.email, organizationId, appConfig.app.uniqueUsersAcrossOrganizations))
            throw UserAlreadyExistException("Email - ${credentials.email} already registered. Unable to create user")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, credentials.userName)
        val user = identityProvider.createUser(
            RequestContext(organizationId = organizationId, requestedPrincipal = createdBy ?: "unknown user", verified),
            identityGroup, credentials
        )
        userRepo.insert(
            Users(
                userHrn.toString(), credentials.email, User.Status.enabled.value,
                organizationId, LocalDateTime.now(), LocalDateTime.now(), verified, false
            )
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

    override suspend fun getUserByEmail(
        organizationId: String?,
        email: String
    ): User {
        val orgId = if (appConfig.app.uniqueUsersAcrossOrganizations) {
            val userRecord = userRepo.findByEmail(email)
                ?: throw EntityNotFoundException("User with email - $email not found")
            userRecord.organizationId
        } else {
            organizationId ?: throw EntityNotFoundException("Organization id is required")
        }
        val org = organizationRepo.findById(orgId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val user = identityProvider.getUserByEmail(identityGroup, email)
        return getUser(ResourceHrn(orgId, "", IamResources.USER, user.username), user)
    }

    override suspend fun changeUserPassword(
        organizationId: String,
        userName: String,
        oldPassword: String,
        newPassword: String
    ): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to authenticate user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.authenticate(identityGroup, userName, oldPassword)
        identityProvider.setUserPassword(identityGroup, userName, newPassword)
        return BaseSuccessResponse(true)
    }

    override suspend fun setUserPassword(
        organizationId: String,
        userName: String,
        password: String
    ): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to set user password")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.setUserPassword(identityGroup, userName, password)
        return BaseSuccessResponse(true)
    }

    override suspend fun listUsers(
        organizationId: String,
        nextToken: String?,
        pageSize: Int?
    ): Triple<List<User>, NextToken?, PaginationOptions> {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val (users, nextToken) = identityProvider.listUsers(
            identityGroup = identityGroup,
            pageToken = nextToken, limit = pageSize
        )
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
        status = if (user.isEnabled) User.Status.enabled else User.Status.disabled,
        verified = user.verified,
        phone = user.phoneNumber,
        createdBy = user.createdBy,
        loginAccess = user.loginAccess
    )

    override suspend fun updateUser(
        organizationId: String,
        userName: String,
        phone: String,
        status: UpdateUserRequest.Status?,
        verified: Boolean?
    ): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val userStatus = status?.toUserStatus()
        val user = identityProvider.updateUser(
            identityGroup = identityGroup,
            userName = userName, phone = phone,
            status = userStatus, verified = verified
        )

        userRepo.update(userHrn.toString(), userStatus, verified)
            ?: throw EntityNotFoundException("User not found")

        return getUser(userHrn, user)
    }

    override suspend fun deleteUser(organizationId: String, userName: String): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to delete user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        if (org.rootUserHrn == userHrn.toString())
            throw BadRequestException("Cannot delete Root User")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.deleteUser(identityGroup, userName)

        userRepo.delete(userHrn.toString()) ?: throw EntityNotFoundException("User not found")

        return BaseSuccessResponse(true)
    }

    override suspend fun authenticate(organizationId: String, userName: String, password: String): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to authenticate user")
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val user = identityProvider.authenticate(identityGroup, userName, password)
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        return getUser(userHrn, user)
    }

    override suspend fun authenticate(email: String, password: String): User {
        val userRecord = userRepo.findByEmail(email)
            ?: throw AuthenticationException("Invalid username and password combination")
        val org = organizationRepo.findById(userRecord.organizationId)
            ?: throw InternalException("Internal error while trying to authenticate the identity.")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val user = identityProvider.authenticate(identityGroup, email, password)
        val userHrn = ResourceHrn(userRecord.organizationId, "", IamResources.USER, user.username)
        return getUser(userHrn, user)
    }
}

/**
 * Service which holds logic related to User operations
 */
interface UsersService {
    suspend fun createUser(
        organizationId: String,
        credentials: PasswordCredentials,
        createdBy: String?,
        verified: Boolean
    ): User

    suspend fun getUser(organizationId: String, userName: String): User
    suspend fun listUsers(organizationId: String, nextToken: String?, pageSize: Int?):
        Triple<List<User>, NextToken?, PaginationOptions>

    suspend fun updateUser(
        organizationId: String,
        userName: String,
        phone: String,
        status: UpdateUserRequest.Status? = null,
        verified: Boolean?
    ): User

    suspend fun deleteUser(organizationId: String, userName: String): BaseSuccessResponse
    suspend fun authenticate(organizationId: String, userName: String, password: String): User
    suspend fun authenticate(email: String, password: String): User
    suspend fun getUserByEmail(organizationId: String?, email: String): User
    suspend fun setUserPassword(organizationId: String, userName: String, password: String): BaseSuccessResponse
    suspend fun changeUserPassword(
        organizationId: String,
        userName: String,
        oldPassword: String,
        newPassword: String
    ): BaseSuccessResponse
}
