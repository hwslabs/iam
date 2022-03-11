@file:Suppress("TooGenericExceptionCaught", "SwallowedException", "TooManyFunctions")

package com.hypto.iam.server.idp

import com.hypto.iam.server.idp.CognitoConstants.ACTION_SUPRESS
import com.hypto.iam.server.idp.CognitoConstants.APP_CLIENT_ID
import com.hypto.iam.server.idp.CognitoConstants.APP_CLIENT_NAME
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_CREATED_BY
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_EMAIL
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_PHONE
import com.hypto.iam.server.idp.CognitoConstants.EMPTY
import com.hypto.iam.server.service.logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

object CognitoConstants {
    const val ATTRIBUTE_EMAIL = "email"
    const val ATTRIBUTE_PHONE = "phone_number"
    const val ATTRIBUTE_CREATED_BY = "createdBy"
    const val ACTION_SUPRESS = "SUPRESS"
    const val APP_CLIENT_NAME = "iam-client"
    const val APP_CLIENT_ID = "iam-client-id"
    const val EMPTY = ""
}

/**
 * Identity provider implementation using Cognito
 */
class CognitoIdentityProviderImpl : IdentityProvider, KoinComponent {
    private val cognitoClient: CognitoIdentityProviderClient by inject()

    override suspend fun createIdentityGroup(name: String, userConfiguration: Configuration): IdentityGroup {
        try {
            // Create user pool
            val createUserPoolResponse =
                cognitoClient.createUserPool(CreateUserPoolRequest.builder().poolName(name).build())
            val createUserPoolClientRequest = CreateUserPoolClientRequest.builder()
                .userPoolId(createUserPoolResponse.userPool().id())
                .clientName(APP_CLIENT_NAME)
                .generateSecret(false)
                .explicitAuthFlows(ExplicitAuthFlowsType.USER_PASSWORD_AUTH, ExplicitAuthFlowsType.ADMIN_NO_SRP_AUTH)
                .build()

            // Create app client to access the user pool
            val appClientResponse = cognitoClient.createUserPoolClient(createUserPoolClientRequest)
            val metadata = mapOf(APP_CLIENT_ID to appClientResponse.userPoolClient().clientId())

            // Return the identity group with user pool and app client details.
            return IdentityGroup(
                createUserPoolResponse.userPool().id(),
                createUserPoolResponse.userPool().name(),
                IdentityProvider.IdentitySource.AWS_COGNITO, metadata
            )
        } catch (e: Exception) {
            logger.error { "Error while trying to create user pool" + e.message }
            throw UnknownError("Unknown error while trying to create identity group")
        }
    }

    override suspend fun deleteIdentityGroup(identityGroup: IdentityGroup) {
        try {
            val deletePoolRequest = DeleteUserPoolRequest.builder().userPoolId(identityGroup.id).build()
            cognitoClient.deleteUserPool(deletePoolRequest)
        } catch (e: Exception) {
            logger.error { "Error while trying to delete user pool" + e.message }
            throw UnknownError("Unknown error while trying to delete identity group")
        }
    }

    override suspend fun createUser(
        context: RequestContext,
        identityGroup: IdentityGroup,
        userCredentials: UserCredentials
    ): User {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        try {
            return when (userCredentials) {
                is PasswordCredentials -> createUserWithPassword(context, identityGroup, userCredentials)
                is AccessTokenCredentials -> createUserWithAccessToken(context, identityGroup, userCredentials)
                else -> throw UnsupportedCredentialsException("Credential type not supported")
            }
        } catch (e: UsernameExistsException) {
            throw UserAlreadyExistException("Username: ${userCredentials.userName} already present in the organization")
        } catch (e: Exception) {
            logger.error { "Error while trying to create user" + e.message }
            throw UnknownError("Unknown error while trying to create user")
        }
    }

    private fun createUserWithAccessToken(
        context: RequestContext,
        identityGroup: IdentityGroup,
        userCredentials: AccessTokenCredentials
    ): User {
        logger.info("Creating cognito user with access token")
        TODO("Not implemented")
    }

    override suspend fun getUser(identityGroup: IdentityGroup, userName: String): User {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        val getUserRequest = AdminGetUserRequest.builder().userPoolId(identityGroup.id).username(userName).build()
        try {
            val response = cognitoClient.adminGetUser(getUserRequest)
            val attrs = response.userAttributes()
            return User(
                response.username(),
                phoneNumber = getAttribute(attrs, ATTRIBUTE_PHONE),
                email = getAttribute(attrs, ATTRIBUTE_EMAIL),
                loginAccess = true,
                isEnabled = response.enabled(),
                createdBy = getAttribute(attrs, ATTRIBUTE_CREATED_BY),
                createdAt = response.userCreateDate().toString()
            )
        } catch (e: UserNotFoundException) {
            logger.info { "Unable to find the user $userName in the organization" }
            throw com.hypto.iam.server.idp.UserNotFoundException("Unable to find the user $userName in the given org")
        } catch (e: Exception) {
            logger.error { "Error while trying to get user information for $userName" + e.message }
            throw UnknownError("Unknown error while trying to get the user information")
        }
    }

    override suspend fun updateUser(
        identityGroup: IdentityGroup,
        userName: String,
        email: String,
        phone: String,
        status: com.hypto.iam.server.models.User.Status?
    ): User {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        if (status == com.hypto.iam.server.models.User.Status.disabled) {
            val request = AdminDisableUserRequest.builder().userPoolId(identityGroup.id).username(userName).build()
            cognitoClient.adminDisableUser(request)
        }
        val attrs = mutableListOf<AttributeType>()
        if (email.isNotEmpty())
            attrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_EMAIL)
                    .value(email).build()
            )
        if (phone.isNotEmpty())
            attrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_PHONE)
                    .value(phone).build()
            )
        val response = AdminUpdateUserAttributesRequest.builder().userAttributes(attrs)
        return getUser(identityGroup, userName)
    }

    private fun createUserWithPassword(
        context: RequestContext,
        identityGroup: IdentityGroup,
        credentials: PasswordCredentials
    ): User {
        val emailAttr = AttributeType.builder()
            .name(ATTRIBUTE_EMAIL)
            .value(credentials.email).build()
        val phoneNumberAttr = AttributeType.builder()
            .name(ATTRIBUTE_PHONE)
            .value(credentials.phoneNumber).build()
        val createdBy = AttributeType.builder()
            .name(ATTRIBUTE_CREATED_BY)
            .value(context.requestedPrincipal).build()

        // Creates user in cognito
        val userRequest = AdminCreateUserRequest.builder()
            .userPoolId(identityGroup.id)
            .username(credentials.userName)
            .temporaryPassword(credentials.password)
            .userAttributes(emailAttr, phoneNumberAttr, createdBy)
            .messageAction(ACTION_SUPRESS) // TODO: Make welcome email as configuration option
            .build()
        val adminCreateUserResponse = cognitoClient.adminCreateUser(userRequest)

        // Initiate auth
        initiateAuth(identityGroup, credentials)

        // Setting the result in User object
        val attrs = adminCreateUserResponse.user().attributes()
        return User(
            adminCreateUserResponse.user().username(),
            phoneNumber = getAttribute(attrs, ATTRIBUTE_PHONE),
            email = getAttribute(attrs, ATTRIBUTE_EMAIL),
            loginAccess = true,
            isEnabled = true,
            createdBy = getAttribute(attrs, ATTRIBUTE_CREATED_BY),
            createdAt = adminCreateUserResponse.user().userCreateDate().toString()
        )
    }

    private fun initiateAuth(
        identityGroup: IdentityGroup,
        credentials: PasswordCredentials
    ) {
        // New user password is marked as temporary and below steps are required to make it permanent
        // Step-1: Initiate an authentication with the temporary password
        // Step-2: Respond to auth challenge by setting the temporary password as permanent password
        val initiateAuthRequest = AdminInitiateAuthRequest.builder()
            .userPoolId(identityGroup.id)
            .clientId(identityGroup.metadata[APP_CLIENT_ID])
            .authParameters(mapOf("USERNAME" to credentials.userName, "PASSWORD" to credentials.password)).build()
        val initiateAuthResponse = cognitoClient.adminInitiateAuth(initiateAuthRequest)

        // Respond to auth challenge flow.
        val adminRespondToAuthRequest = AdminRespondToAuthChallengeRequest.builder()
            .userPoolId(identityGroup.id)
            .clientId(identityGroup.metadata[APP_CLIENT_ID])
            .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
            .session(initiateAuthResponse.session())
            .challengeResponses(mapOf("USERNAME" to credentials.userName, "PASSWORD" to credentials.password)).build()
        cognitoClient.adminRespondToAuthChallenge(adminRespondToAuthRequest)
    }

    override suspend fun listUsers(
        identityGroup: IdentityGroup,
        pageToken: String?,
        limit: Int?
    ): Pair<List<User>, NextToken?> {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        try {
            val listRequest = ListUsersRequest.builder().userPoolId(identityGroup.id).limit(limit)
            if (pageToken != null)
                listRequest.paginationToken(pageToken)
            val response = cognitoClient.listUsers(listRequest.build())
            val usersTypes = response.users()
            val users = usersTypes.map { user ->
                User(
                    user.username(),
                    email = getAttribute(user.attributes(), ATTRIBUTE_EMAIL),
                    phoneNumber = getAttribute(user.attributes(), ATTRIBUTE_PHONE),
                    loginAccess = true,
                    isEnabled = user.userStatus() != UserStatusType.ARCHIVED,
                    createdBy = getAttribute(user.attributes(), ATTRIBUTE_CREATED_BY),
                    createdAt = user.userCreateDate().toString()
                )
            }.toList()
            return Pair(users, response.paginationToken())
        } catch (e: Exception) {
            logger.error {
                "Error while trying to list users" +
                    " from the identity pool ${identityGroup.id}" + e.message
            }
            throw UnknownError("Unknown error while trying to list users")
        }
    }

    override suspend fun deleteUser(identityGroup: IdentityGroup, userName: String) {
        try {
            require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
            val adminDeleteUserRequest = AdminDeleteUserRequest.builder()
                .userPoolId(identityGroup.id).username(userName).build()
            cognitoClient.adminDeleteUser(adminDeleteUserRequest)
        } catch (e: Exception) {
            logger.error {
                "Error while trying to delete user $userName" +
                    " from the identity pool ${identityGroup.id}" + e.message
            }
            throw UnknownError("Unknown error while trying to delete user: $userName")
        }
    }

    override suspend fun getIdentitySource(): IdentityProvider.IdentitySource {
        return IdentityProvider.IdentitySource.AWS_COGNITO
    }

    private fun getAttribute(attrs: List<AttributeType>, key: String): String {
        return try {
            attrs.first { attr -> attr.name() == key }.value()
        } catch (e: NoSuchElementException) {
            EMPTY
        }
    }
}
