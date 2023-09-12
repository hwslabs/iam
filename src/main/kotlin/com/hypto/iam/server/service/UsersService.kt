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
import com.hypto.iam.server.extensions.toUTCOffset
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
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.validators.EMAIL_REGEX
import com.txman.TxMan
import io.ktor.server.plugins.BadRequestException
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UsersServiceImpl : KoinComponent, UsersService {
    private val principalPolicyService: PrincipalPolicyService by inject()
    private val hrnFactory: HrnFactory by inject()
    private val userRepo: UserRepo by inject()
    private val organizationRepo: OrganizationRepo by inject()
    private val organizationService: OrganizationsService by inject()
    private val identityProvider: IdentityProvider by inject()
    private val gson: Gson by inject()
    private val txMan: TxMan by inject()
    private val appConfig: AppConfig by inject()
    private val passcodeRepo: PasscodeRepo by inject()

    @Suppress("CyclomaticComplexMethod")
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
        verified: Boolean,
        policies: List<String>?
    ): User {
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
                    " already registered. Unable to create user"
            )
        }

        return txMan.wrap {
            if (password != null && loginAccess) {
                createUserInIdentityProvider(
                    username,
                    preferredUsername,
                    name,
                    email,
                    phoneNumber,
                    password,
                    organizationId,
                    createdBy,
                    verified
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

            policies?.let {
                principalPolicyService.attachPoliciesToUser(
                    ResourceHrn(userHrn.toString()),
                    policies.map { hrnFactory.getHrn(it) }
                )
            }
            if (password != null) {
                email?.let {
                    passcodeRepo.deleteByEmailAndPurpose(email, VerifyEmailRequest.Purpose.invite)
                }
            }
            getUser(userHrn, userRecord)
        }
    }

    @Suppress("ThrowsCount")
    private suspend fun createUserInIdentityProvider(
        username: String,
        preferredUsername: String?,
        name: String?,
        email: String?,
        phoneNumber: String?,
        password: String?,
        organizationId: String,
        createdBy: String?,
        verified: Boolean
    ) {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id")

        val passwordCredentials = PasswordCredentials(
            username = username,
            preferredUsername = preferredUsername,
            name = name,
            email = email ?: throw BadRequestException("Email is required"),
            phoneNumber = phoneNumber ?: "",
            password = password ?: throw BadRequestException("Password is required"),
        )

        identityProvider.createUser(
            RequestContext(
                organizationId = organizationId,
                requestedPrincipal = createdBy ?: "unknown user",
                verified
            ),
            gson.fromJson(org.metadata.data(), IdentityGroup::class.java),
            passwordCredentials
        )
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
        if (userRecord.loginAccess != true) {
            throw BadRequestException("User - $userName does not have login access")
        }

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.authenticate(identityGroup, userName, oldPassword)
        identityProvider.setUserPassword(identityGroup, userName, newPassword)
        return BaseSuccessResponse(true)
    }

    override suspend fun setUserPassword(
        organizationId: String,
        user: User,
        password: String
    ): BaseSuccessResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to set user password")

        val userHrn = ResourceHrn(user.hrn)
        val userRecord = userRepo.findByHrn(userHrn.toString())
            ?: throw EntityNotFoundException("Unable to find user")
        if (userRecord.loginAccess != true) {
            throw BadRequestException("User - ${userHrn.resourceInstance} does not have login access")
        }

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.setUserPassword(identityGroup, user.username, password)
        passcodeRepo.deleteByEmailAndPurpose(user.email!!, VerifyEmailRequest.Purpose.reset)
        return BaseSuccessResponse(true)
    }

    override suspend fun createUserPassword(
        organizationId: String,
        userId: String,
        password: String
    ): BaseSuccessResponse {
        val user = getUser(organizationId, userId)
        val cognito = appConfig.cognito
        val organization = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to create user")
        if (organization.metadata != null) {
            throw BadRequestException("Organization already has password access")
        }
        organizationService.updateOrganization(organizationId, null, null, cognito)
        createUserInIdentityProvider(
            user.username,
            user.preferredUsername,
            user.name,
            user.email,
            user.phone,
            password,
            organizationId,
            user.createdBy,
            user.verified
        )
        return BaseSuccessResponse(true)
    }

    override suspend fun listUsers(
        organizationId: String,
        paginationContext: PaginationContext
    ): UserPaginatedResponse {
        val org = organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")

        val users = userRepo.fetchUsers(organizationId, paginationContext).map { user ->
            getUser(ResourceHrn(user.hrn), user)
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
        name = user.name ?: "",
        organizationId = userHrn.organization,
        email = user.email,
        status = if (user.status == User.Status.enabled.value) User.Status.enabled else User.Status.disabled,
        verified = user.verified,
        createdBy = user.createdBy ?: "",
        createdAt = user.createdAt.toUTCOffset(),
        loginAccess = user.loginAccess ?: false
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
        if (userRecord.loginAccess == true) {
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

        if (userRecord.loginAccess == true) {
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
        if (userRecord.loginAccess != true) {
            throw BadRequestException("User - $userName does not have login access")
        }

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

        if (userRecord.loginAccess == null) {
            throw BadRequestException("User - $username does not have login access")
        }

        if (org.metadata == null) {
            throw AuthenticationException("User - $username does not have password access")
        }

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        val user = identityProvider.authenticate(identityGroup, username, password)
        val userHrn = ResourceHrn(userRecord.organizationId, "", IamResources.USER, user.username)
        return getUser(userHrn, userRecord)
    }
}

/**
 * Service which holds logic related to User operations
 */
@Suppress("TooManyFunctions")
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
        verified: Boolean,
        policies: List<String>? = null
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
        password: String
    ): BaseSuccessResponse

    suspend fun createUserPassword(
        organizationId: String,
        userId: String,
        password: String
    ): BaseSuccessResponse

    suspend fun changeUserPassword(
        organizationId: String,
        userName: String,
        oldPassword: String,
        newPassword: String
    ): BaseSuccessResponse
}
