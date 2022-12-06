@file:Suppress("LongParameterList")

package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.toUserStatus
import com.hypto.iam.server.idp.IdentityGroup
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.idp.PasswordCredentials
import com.hypto.iam.server.idp.RequestContext
import com.hypto.iam.server.idp.UserAlreadyExistException
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.UserPaginatedResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.validators.EMAIL_REGEX
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
    private val passcodeRepo: PasscodeRepo by inject()

    override suspend fun createUser(
        organizationId: String,
        loginAccess: Boolean,
        username: String,
        preferredUsername: String?,
        name: String?,
        email: String?,
        phoneNumber: String?,
        password: String?,
        createdBy: String?,
        verified: Boolean
    ): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to create a user")

        if (userRepo.existsByAliasUsername(
                preferredUsername,
                email,
                organizationId,
                appConfig.app.uniqueUsersAcrossOrganizations
            )
        ) {
            throw UserAlreadyExistException(
                email?.let { "Email - $email" }.orEmpty() +
                    preferredUsername?.let { "Username - $preferredUsername" }.orEmpty() +
                    "already registered. Unable to create user"
            )
        }

        if (loginAccess) {
            val passwordCredentials = PasswordCredentials(
                username = username,
                preferredUsername = preferredUsername,
                name = name,
                email = email!!,
                phoneNumber = phoneNumber ?: "",
                password = password!!
            )
            val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
            identityProvider.createUser(
                RequestContext(
                    organizationId = organizationId,
                    requestedPrincipal = createdBy ?: "unknown user", verified
                ),
                identityGroup, passwordCredentials
            )
        }
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, username)

        val userRecord = userRepo.insert(
            UsersRecord().apply {
                this.hrn = userHrn.toString()
                this.organizationId = organizationId
                this.email = email
                this.status = User.Status.enabled.value
                this.verified = verified
                this.deleted = false
                this.createdAt = LocalDateTime.now()
                this.updatedAt = LocalDateTime.now()
                this.preferredUsername = preferredUsername
                this.loginAccess = loginAccess
                this.name = name
                this.createdBy = createdBy
            }
        ) ?: throw InternalException("Unable to create user")
        return getUser(userHrn, userRecord)
    }

    override suspend fun getUser(organizationId: String, userName: String): User {
        organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id. Unable to get user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        val userRecord = userRepo.findByHrn(userHrn.toString())
            ?: throw EntityNotFoundException("Unable to find user")
        return getUser(userHrn, userRecord)
    }

    override suspend fun getUserByEmail(
        organizationId: String?,
        email: String
    ): User {
        val userRecord = if (appConfig.app.uniqueUsersAcrossOrganizations) {
            val user = userRepo.findByEmail(email)
                ?: throw EntityNotFoundException("User with email - $email not found")
            organizationRepo.findById(user.organizationId)
                ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
            user
        } else {
            organizationId ?: throw EntityNotFoundException("Organization id is required")
            userRepo.findByEmail(email, organizationId)
        } ?: throw EntityNotFoundException("User with email - $email not found")

        return getUser(ResourceHrn(userRecord.hrn), userRecord)
    }

    override suspend fun changeUserPassword(
        organizationId: String,
        userName: String,
        oldPassword: String,
        newPassword: String
    ): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to authenticate user")

        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        val userRecord = userRepo.findByHrn(userHrn.toString())
            ?: throw EntityNotFoundException("Unable to find user")
        if (!userRecord.loginAccess)
            throw BadRequestException("User - $userName does not have login access")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.authenticate(identityGroup, userName, oldPassword)
        identityProvider.setUserPassword(identityGroup, userName, newPassword)
        return BaseSuccessResponse(true)
    }

    override suspend fun setUserPassword(
        organizationId: String,
        user: User,
        password: String,
        passcodeStr: String
    ): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to set user password")

        val userHrn = ResourceHrn(user.hrn)
        val userRecord = userRepo.findByHrn(userHrn.toString())
            ?: throw EntityNotFoundException("Unable to find user")
        if (!userRecord.loginAccess)
            throw BadRequestException("User - ${userHrn.resourceInstance} does not have login access")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.setUserPassword(identityGroup, user.username, password)
        passcodeRepo.deleteByEmailAndPurpose(user.email!!, VerifyEmailRequest.Purpose.reset)
        return BaseSuccessResponse(true)
    }

    override suspend fun listUsers(
        organizationId: String,
        paginationContext: PaginationContext
    ): UserPaginatedResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
<<<<<<< HEAD

        val users = userRepo.fetchUsers(organizationId, paginationContext).map { user ->
            getUser(ResourceHrn(user.hrn), user)
=======
        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val (users, nextToken) = identityProvider.listUsers(
            identityGroup = identityGroup,
            pageToken = nextToken,
            limit = pageSize
        )
        val pageContext = PaginationOptions(pageSize)
        val externalUserTypeUsers = users.map { user ->
            val userHrn = ResourceHrn(organizationId, "", IamResources.USER, user.username)
            getUser(userHrn, user)
>>>>>>> b681275 (Upgrade dependencies and formatting changes)
        }.toList()

        val newContext = PaginationContext.from(users.lastOrNull()?.hrn, paginationContext)
        return UserPaginatedResponse(
            users,
            newContext.nextToken,
            newContext.toOptions()
        )
    }

    private fun getUser(
        userHrn: ResourceHrn,
        user: UsersRecord
    ) = User(
        hrn = userHrn.toString(),
        username = userHrn.resourceInstance.toString(),
        preferredUsername = user.preferredUsername,
        name = user.name,
        organizationId = userHrn.organization,
        email = user.email,
        status = if (user.status == User.Status.enabled.value) User.Status.enabled else User.Status.disabled,
        verified = user.verified,
        createdBy = user.createdBy,
        loginAccess = user.loginAccess
    )

    override suspend fun updateUser(
        organizationId: String,
        userName: String,
        name: String?,
        phone: String?,
        status: UpdateUserRequest.Status?,
        verified: Boolean?
    ): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        val userRecord = userRepo.findByHrn(userHrn.toString())
            ?: throw EntityNotFoundException("User not found")

        val userStatus = status?.toUserStatus()
        if (userRecord.loginAccess) {
            val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)

            identityProvider.updateUser(
                identityGroup = identityGroup,
                name = name,
                userName = userName,
                phone = phone,
                status = userStatus,
                verified = verified
            )
        }
        val updatedUserRecord = userRepo.update(userHrn.toString(), userStatus, verified, name)
            ?: throw EntityNotFoundException("User not found")

        return getUser(userHrn, updatedUserRecord)
    }

    override suspend fun deleteUser(organizationId: String, userName: String): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to delete user")
        val userHrn = ResourceHrn(organizationId, "", IamResources.USER, userName)
        if (org.rootUserHrn == userHrn.toString()) {
            throw BadRequestException("Cannot delete Root User")
        }

        val userRecord = userRepo.findByHrn(userHrn.toString())
            ?: throw EntityNotFoundException("User not found")

        if (userRecord.loginAccess) {
            val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
            identityProvider.deleteUser(identityGroup, userName)
        }
        userRepo.delete(userHrn.toString())

        return BaseSuccessResponse(true)
    }

    override suspend fun authenticate(organizationId: String, userName: String, password: String): User {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to authenticate user")

        val userRecord = userRepo.findByEmail(userName, organizationId)
            ?: throw EntityNotFoundException("User not found")
        if (!userRecord.loginAccess)
            throw BadRequestException("User - $userName does not have login access")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.authenticate(identityGroup, userName, password)

        return getUser(ResourceHrn(userRecord.hrn), userRecord)
    }

    override suspend fun authenticate(username: String, password: String): User {
        val userRecord = if (EMAIL_REGEX.toRegex().matches(username)) {
            userRepo.findByEmail(username)
        } else {
            userRepo.findByPreferredUsername(username)
        } ?: throw AuthenticationException("Invalid username and password combination")

        val org = organizationRepo.findById(userRecord.organizationId)
            ?: throw InternalException("Internal error while trying to authenticate the identity.")

        if (!userRecord.loginAccess)
            throw BadRequestException("User - $username does not have login access")

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val user = identityProvider.authenticate(identityGroup, username, password)
        val userHrn = ResourceHrn(userRecord.organizationId, "", IamResources.USER, user.username)
        return getUser(userHrn, userRecord)
    }
}

/**
 * Service which holds logic related to User operations
 */
interface UsersService {
    suspend fun createUser(
        organizationId: String,
        loginAccess: Boolean,
        username: String,
        preferredUsername: String?,
        name: String?,
        email: String?,
        phoneNumber: String?,
        password: String?,
        createdBy: String?,
        verified: Boolean
    ): User

    suspend fun getUser(organizationId: String, userName: String): User
    suspend fun listUsers(organizationId: String, paginationContext: PaginationContext):
        UserPaginatedResponse

    suspend fun updateUser(
        organizationId: String,
        userName: String,
        name: String?,
        phone: String?,
        status: UpdateUserRequest.Status? = null,
        verified: Boolean?
    ): User

    suspend fun deleteUser(organizationId: String, userName: String): BaseSuccessResponse
    suspend fun authenticate(organizationId: String, userName: String, password: String): User
    suspend fun authenticate(username: String, password: String): User
    suspend fun getUserByEmail(organizationId: String?, email: String): User
    suspend fun setUserPassword(
        organizationId: String,
        user: User,
        password: String,
        passcodeStr: String
    ): BaseSuccessResponse

    suspend fun changeUserPassword(
        organizationId: String,
        userName: String,
        oldPassword: String,
        newPassword: String
    ): BaseSuccessResponse
}
