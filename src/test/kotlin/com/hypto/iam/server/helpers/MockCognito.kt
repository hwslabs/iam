package com.hypto.iam.server.helpers

import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import org.koin.test.KoinTest
import org.koin.test.mock.declareMock
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AddCustomAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType

fun KoinTest.mockCognitoClient(): CognitoIdentityProviderClient {
    return declareMock {
        coEvery { this@declareMock.createUserPool(any<CreateUserPoolRequest>()) } coAnswers {
            val result = CreateUserPoolResponse.builder()
                .userPool(UserPoolType.builder().id("testUserPoolId").name("testUserPoolName").build())
                .build()
            result
        }
        coEvery { this@declareMock.createUserPoolClient(any<CreateUserPoolClientRequest>()) } coAnswers {
            CreateUserPoolClientResponse.builder()
                .userPoolClient(UserPoolClientType.builder().clientId("12345").build())
                .build()
        }
        coEvery { this@declareMock.deleteUserPool(any<DeleteUserPoolRequest>()) } returns DeleteUserPoolResponse
            .builder().build()
        coEvery { this@declareMock.adminGetUser(any<AdminGetUserRequest>()) } coAnswers {
            AdminGetUserResponse.builder()
                .enabled(true)
                .userAttributes(listOf())
                .username(firstArg<AdminGetUserRequest>().username())
                .userCreateDate(Instant.now())
                .build()
        }
        coEvery { this@declareMock.adminDisableUser(any<AdminDisableUserRequest>()) } returns mockk()
        coEvery { this@declareMock.adminEnableUser(any<AdminEnableUserRequest>()) } returns mockk()
        coEvery { this@declareMock.adminCreateUser(any<AdminCreateUserRequest>()) } coAnswers {
            AdminCreateUserResponse.builder()
                .user(
                    UserType.builder().attributes(listOf())
                    .username(firstArg<AdminCreateUserRequest>().username())
                    .userCreateDate(Instant.now())
                    .attributes(firstArg<AdminCreateUserRequest>().userAttributes())
                    .build())
                .build()
        }
        coEvery { this@declareMock.adminInitiateAuth(any<AdminInitiateAuthRequest>()) } coAnswers {
            AdminInitiateAuthResponse.builder()
                .session("").build()
        }
        coEvery { this@declareMock.adminRespondToAuthChallenge(
            any<AdminRespondToAuthChallengeRequest>()) } returns mockk()
        val listUsersResponse = ListUsersResponse.builder().users(listOf()).build()
        coEvery { this@declareMock.listUsers(any<ListUsersRequest>()) } returns listUsersResponse
        coEvery { this@declareMock.adminDeleteUser(any<AdminDeleteUserRequest>()) } returns mockk()
        coEvery { this@declareMock.addCustomAttributes(any<AddCustomAttributesRequest>()) } returns mockk()
        coEvery { this@declareMock.adminUpdateUserAttributes(any<AdminUpdateUserAttributesRequest>()) } returns mockk()
    }
}
