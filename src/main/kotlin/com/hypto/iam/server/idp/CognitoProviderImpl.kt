@file:Suppress("TooGenericExceptionCaught", "SwallowedException", "TooManyFunctions")

package com.hypto.iam.server.idp

import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.exceptions.UnknownException
import com.hypto.iam.server.idp.CognitoConstants.ACTION_SUPPRESS
import com.hypto.iam.server.idp.CognitoConstants.APP_CLIENT_ID
import com.hypto.iam.server.idp.CognitoConstants.APP_CLIENT_NAME
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_CREATED_BY
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_EMAIL
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_EMAIL_VERIFIED
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_NAME
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_PHONE
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_PREFIX_CUSTOM
import com.hypto.iam.server.idp.CognitoConstants.ATTRIBUTE_VERIFIED
import com.hypto.iam.server.idp.CognitoConstants.EMPTY
import com.hypto.iam.server.security.AuthenticationException
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AddCustomAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AliasAttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeDataType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.SchemaAttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.StringAttributeConstraintsType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

private val logger = KotlinLogging.logger {}

object CognitoConstants {
    const val ATTRIBUTE_NAME = "name"
    const val ATTRIBUTE_EMAIL = "email"
    const val ATTRIBUTE_EMAIL_VERIFIED = "email_verified"
    const val ATTRIBUTE_PHONE = "phone_number"
    const val ATTRIBUTE_CREATED_BY = "createdBy"
    const val ATTRIBUTE_PREFERRED_USERNAME = "preferred_username"
    const val ATTRIBUTE_VERIFIED = "verified"
    const val ATTRIBUTE_PREFIX_CUSTOM = "custom:"
    const val ACTION_SUPPRESS = "SUPPRESS"
    const val APP_CLIENT_NAME = "iam-client"
    const val APP_CLIENT_ID = "iam-client-id"
    const val EMPTY = ""
}

/**
 * Identity provider implementation using Cognito
 */
class CognitoIdentityProviderImpl : IdentityProvider, KoinComponent {
    private val cognitoClient: CognitoIdentityProviderClient by inject()
    private val aliasAttributeTypes = mutableListOf(AliasAttributeType.EMAIL, AliasAttributeType.PREFERRED_USERNAME)

    override suspend fun createIdentityGroup(name: String, configuration: Configuration): IdentityGroup {
        try {
            // Create user pool
            val createUserPoolResponse = cognitoClient.createUserPool(
                CreateUserPoolRequest.builder().poolName(name)
                    .aliasAttributes(aliasAttributeTypes)
                    .build()
            )
            val createUserPoolClientRequest = CreateUserPoolClientRequest.builder()
                .userPoolId(createUserPoolResponse.userPool().id())
                .clientName(APP_CLIENT_NAME)
                .generateSecret(false)
                .explicitAuthFlows(ExplicitAuthFlowsType.USER_PASSWORD_AUTH, ExplicitAuthFlowsType.ADMIN_NO_SRP_AUTH)
                .build()

            // Add custom attribute schema for the created user pool
            val customAttributeRequest = AddCustomAttributesRequest.builder().userPoolId(
                createUserPoolResponse
                    .userPool().id()
            ).customAttributes(
                SchemaAttributeType.builder().name(ATTRIBUTE_CREATED_BY)
                    .attributeDataType(AttributeDataType.STRING)
                    .stringAttributeConstraints(
                        StringAttributeConstraintsType.builder()
                            .maxLength("100")
                            .minLength("3").build()
                    )
                    .build(),
                SchemaAttributeType.builder().name(ATTRIBUTE_VERIFIED)
                    .attributeDataType(AttributeDataType.BOOLEAN)
                    .build()
            )
                .build()
            cognitoClient.addCustomAttributes(customAttributeRequest)

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
            logger.error(e) { "Error while trying to create user pool with message = ${e.message}" }
            throw UnknownException("Unknown error while trying to create identity group")
        }
    }

    override suspend fun deleteIdentityGroup(identityGroup: IdentityGroup) {
        try {
            val deletePoolRequest = DeleteUserPoolRequest.builder().userPoolId(identityGroup.id).build()
            cognitoClient.deleteUserPool(deletePoolRequest)
        } catch (e: Exception) {
            logger.error(e) { "Error while trying to delete user pool with message = ${e.message}" }
            throw UnknownException("Unknown error while trying to delete identity group")
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
            logger.info(e) { "Error while trying to create user = ${e.message}" }
            // We get same error for duplicate username as well as duplicate preferredUsername
            // If we can fetch the user, with the same username,
            //  then we throw an exception as our random generated username already exists
            // Or else we throw a exception stating duplicate preferredUsername already exists.
            try {
                getUser(identityGroup, userCredentials.username)
                throw UnknownError("Unknown error while trying to create user")
            } catch (e: com.hypto.iam.server.idp.UserNotFoundException) {
                throw UserAlreadyExistException("Preferred username already present in the organization")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while trying to create user = ${e.message}" }
            throw UnknownException("Unknown error while trying to create user")
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

    override suspend fun getUser(identityGroup: IdentityGroup, userName: String, isAliasUsername: Boolean): User {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        val getUserRequest = AdminGetUserRequest.builder().userPoolId(identityGroup.id).username(userName).build()
        try {
            val response = cognitoClient.adminGetUser(getUserRequest)

            // Adding this check as admin-get-user will succeed with alias usernames
            // We will be supporting aliasUsernames only for authentication purposes
            if (!isAliasUsername && response.username() != userName) {
                throw UserNotFoundException("User not found")
            }

            val attrs = response.userAttributes()
            return User(
                username = response.username(),
                preferredUsername = getAttribute(attrs, ATTRIBUTE_PREFERRED_USERNAME),
                name = getAttribute(attrs, ATTRIBUTE_NAME),
                phoneNumber = getAttribute(attrs, ATTRIBUTE_PHONE),
                email = getAttribute(attrs, ATTRIBUTE_EMAIL),
                verified = getAttribute(attrs, ATTRIBUTE_EMAIL_VERIFIED).toBoolean(),
                loginAccess = true,
                isEnabled = response.enabled(),
                createdBy = getAttribute(attrs, ATTRIBUTE_PREFIX_CUSTOM + ATTRIBUTE_CREATED_BY),
                createdAt = response.userCreateDate().toString()
            )
        } catch (e: UserNotFoundException) {
            logger.info(e) { "Unable to find the user $userName in the organization" }
            throw UserNotFoundException("Unable to find the user $userName in the given org")
        } catch (e: Exception) {
            logger.error(e) { "Error while trying to get user information for $userName" + e.message }
            throw UnknownException("Unknown error while trying to get the user information")
        }
    }

    override suspend fun getUserByEmail(identityGroup: IdentityGroup, email: String): User {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        try {
            val listRequest =
                ListUsersRequest.builder().userPoolId(identityGroup.id).filter("email = \"$email\"").limit(1)
            val response = cognitoClient.listUsers(listRequest.build())
            val userType = response.users().first()
            val attrs = userType.attributes()
            return User(
                username = userType.username(),
                preferredUsername = getAttribute(attrs, ATTRIBUTE_PREFERRED_USERNAME),
                name = getAttribute(attrs, ATTRIBUTE_NAME),
                email = getAttribute(attrs, ATTRIBUTE_EMAIL),
                phoneNumber = getAttribute(attrs, ATTRIBUTE_PHONE),
                verified = getAttribute(attrs, ATTRIBUTE_EMAIL_VERIFIED).toBoolean(),
                loginAccess = true,
                isEnabled = userType.enabled(),
                createdBy = getAttribute(attrs, ATTRIBUTE_PREFIX_CUSTOM + ATTRIBUTE_CREATED_BY),
                createdAt = userType.userCreateDate().toString()
            )
        } catch (e: NoSuchElementException) {
            logger.info(e) { "Unable to find the user with email $email in the organization" }
            throw UserNotFoundException("Unable to find the user $email in the given organization")
        } catch (e: Exception) {
            logger.error(e) { "Error while trying to get user information with email $email" + e.message }
            throw UnknownException("Unknown error while trying to get the user information")
        }
    }

    override suspend fun updateUser(
        identityGroup: IdentityGroup,
        userName: String,
        name: String?,
        phone: String?,
        status: com.hypto.iam.server.models.User.Status?,
        verified: Boolean?
    ): User {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)

        when (status) {
            com.hypto.iam.server.models.User.Status.disabled -> {
                val request =
                    AdminDisableUserRequest.builder().userPoolId(identityGroup.id).username(userName).build()
                cognitoClient.adminDisableUser(request)
            }

            com.hypto.iam.server.models.User.Status.enabled -> {
                val request =
                    AdminEnableUserRequest.builder().userPoolId(identityGroup.id).username(userName).build()
                cognitoClient.adminEnableUser(request)
            }

            else -> {} // Do not take any action if Status is NULL
        }

        val attrs = mutableListOf<AttributeType>()
        if (!name.isNullOrEmpty())
            attrs.add(AttributeType.builder().name(ATTRIBUTE_NAME).value(name).build())
        if (!phone.isNullOrEmpty())
            attrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_PHONE)
                    .value(phone).build()
            )
        if (verified != null)
            attrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_EMAIL_VERIFIED)
                    .value(verified.toString()).build()
            )
        val updateRequest =
            AdminUpdateUserAttributesRequest.builder().userPoolId(identityGroup.id).username(userName)
                .userAttributes(attrs).build()
        cognitoClient.adminUpdateUserAttributes(updateRequest)

        return getUser(identityGroup, userName)
    }

    override suspend fun setUserPassword(
        identityGroup: IdentityGroup,
        userName: String,
        password: String
    ) {
        require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
        val setPasswordRequest =
            AdminSetUserPasswordRequest.builder().password(password).userPoolId(identityGroup.id).username(userName)
                .build()
        cognitoClient.adminSetUserPassword(setPasswordRequest)
    }

    private fun createUserWithPassword(
        context: RequestContext,
        identityGroup: IdentityGroup,
        credentials: PasswordCredentials
    ): User {
        val emailAttr = AttributeType.builder()
            .name(ATTRIBUTE_EMAIL)
            .value(credentials.email).build()
        val emailVerifiedAttr = AttributeType.builder()
            .name(ATTRIBUTE_EMAIL_VERIFIED)
            .value(context.verified.toString()).build()
        val createdBy = AttributeType.builder()
            .name(ATTRIBUTE_PREFIX_CUSTOM + ATTRIBUTE_CREATED_BY)
            .value(context.requestedPrincipal).build()

        val optionalUserAttrs = mutableListOf<AttributeType>()
        if (!credentials.phoneNumber.isNullOrEmpty()) {
            optionalUserAttrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_PHONE)
                    .value(credentials.phoneNumber).build()
            )
        }
        if (!credentials.preferredUsername.isNullOrEmpty()) {
            optionalUserAttrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_PREFERRED_USERNAME)
                    .value(credentials.preferredUsername).build()
            )
        }
        if (!credentials.name.isNullOrEmpty()) {
            optionalUserAttrs.add(
                AttributeType.builder()
                    .name(ATTRIBUTE_NAME)
                    .value(credentials.name).build()
            )
        }

        // Creates user in cognito
        val userRequest = AdminCreateUserRequest.builder()
            .userPoolId(identityGroup.id)
            .username(credentials.username)
            .temporaryPassword(credentials.password)
            .userAttributes(emailAttr, emailVerifiedAttr, createdBy, *optionalUserAttrs.toTypedArray())
            .messageAction(ACTION_SUPPRESS) // TODO: Make welcome email as configuration option
            .build()

        val adminCreateUserResponse = cognitoClient.adminCreateUser(userRequest)

        // Initiate auth
        initiateNewUserAuth(identityGroup, credentials)

        // Setting the result in User object
        val attrs = adminCreateUserResponse.user().attributes()
        return User(
            username = adminCreateUserResponse.user().username(),
            preferredUsername = getAttribute(attrs, ATTRIBUTE_PREFERRED_USERNAME),
            name = getAttribute(attrs, ATTRIBUTE_NAME),
            phoneNumber = getAttribute(attrs, ATTRIBUTE_PHONE),
            email = getAttribute(attrs, ATTRIBUTE_EMAIL),
            verified = getAttribute(attrs, ATTRIBUTE_EMAIL_VERIFIED).toBoolean(),
            loginAccess = true,
            isEnabled = true,
            createdBy = getAttribute(attrs, ATTRIBUTE_PREFIX_CUSTOM + ATTRIBUTE_CREATED_BY),
            createdAt = adminCreateUserResponse.user().userCreateDate().toString()
        )
    }

    private fun initiateNewUserAuth(
        identityGroup: IdentityGroup,
        credentials: PasswordCredentials
    ) {
        // New user password is marked as temporary and below steps are required to make it permanent
        // Step-1: Initiate an authentication with the temporary password
        // Step-2: Respond to auth challenge by setting the temporary password as permanent password
        val initiateAuthRequest = AdminInitiateAuthRequest.builder()
            .userPoolId(identityGroup.id)
            .clientId(identityGroup.metadata[APP_CLIENT_ID])
            .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
            .authParameters(mapOf("USERNAME" to credentials.username, "PASSWORD" to credentials.password)).build()
        val initiateAuthResponse = cognitoClient.adminInitiateAuth(initiateAuthRequest)

        // Respond to auth challenge flow.
        val adminRespondToAuthRequest = AdminRespondToAuthChallengeRequest.builder()
            .userPoolId(identityGroup.id)
            .clientId(identityGroup.metadata[APP_CLIENT_ID])
            .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
            .session(initiateAuthResponse.session())
            .challengeResponses(mapOf("USERNAME" to credentials.username, "NEW_PASSWORD" to credentials.password))
            .build()
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
                val attrs = user.attributes()
                User(
                    username = user.username(),
                    preferredUsername = getAttribute(attrs, ATTRIBUTE_PREFERRED_USERNAME),
                    name = getAttribute(attrs, ATTRIBUTE_NAME),
                    email = getAttribute(attrs, ATTRIBUTE_EMAIL),
                    verified = getAttribute(attrs, ATTRIBUTE_EMAIL_VERIFIED).toBoolean(),
                    phoneNumber = getAttribute(attrs, ATTRIBUTE_PHONE),
                    loginAccess = true,
                    isEnabled = user.userStatus() != UserStatusType.ARCHIVED,
                    createdBy = getAttribute(attrs, ATTRIBUTE_PREFIX_CUSTOM + ATTRIBUTE_CREATED_BY),
                    createdAt = user.userCreateDate().toString()
                )
            }.toList()
            return Pair(users, response.paginationToken())
        } catch (e: Exception) {
            logger.error(e) {
                "Error while trying to list users" +
                    " from the identity pool ${identityGroup.id}" + e.message
            }
            throw UnknownException("Unknown error while trying to list users")
        }
    }

    override suspend fun deleteUser(identityGroup: IdentityGroup, userName: String) {
        try {
            require(identityGroup.identitySource == IdentityProvider.IdentitySource.AWS_COGNITO)
            val adminDeleteUserRequest = AdminDeleteUserRequest.builder()
                .userPoolId(identityGroup.id).username(userName).build()
            cognitoClient.adminDeleteUser(adminDeleteUserRequest)
        } catch (e: Exception) {
            logger.error(e) {
                "Error while trying to delete user $userName" +
                    " from the identity pool ${identityGroup.id}" + e.message
            }
            throw UnknownException("Unknown error while trying to delete user: $userName")
        }
    }

    override suspend fun getIdentitySource(): IdentityProvider.IdentitySource {
        return IdentityProvider.IdentitySource.AWS_COGNITO
    }

    override suspend fun authenticate(identityGroup: IdentityGroup, userName: String, password: String): User {
        try {
            val initiateAuthRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(identityGroup.id)
                .clientId(identityGroup.metadata[APP_CLIENT_ID])
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .authParameters(mapOf("USERNAME" to userName, "PASSWORD" to password)).build()
            val initiateAuthResponse = cognitoClient.adminInitiateAuth(initiateAuthRequest)
            return getUser(identityGroup, userName, true)
        } catch (e: NotAuthorizedException) {
            logger.info { "Invalid username/password combo from user" }
            throw AuthenticationException("Invalid username and password combination")
        } catch (e: UserNotFoundException) {
            logger.info(e) { "Unable to find the user $userName" }
            throw AuthenticationException("Invalid username and password combination")
        } catch (e: Exception) {
            logger.error(e) { "Error while trying to authenticate from cognito" }
            throw InternalException("Internal error while trying to authenticate the identity.")
        }
    }

    private fun getAttribute(attrs: List<AttributeType>, key: String): String {
        return try {
            attrs.first { attr -> attr.name() == key }.value()
        } catch (e: NoSuchElementException) {
            EMPTY
        }
    }
}
