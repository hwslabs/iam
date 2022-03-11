package com.hypto.iam.server.di

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.repositories.UserAuthProvidersRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.idp.CognitoIdentityProviderImpl
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.service.ActionService
import com.hypto.iam.server.service.ActionServiceImpl
import com.hypto.iam.server.service.CredentialService
import com.hypto.iam.server.service.CredentialServiceImpl
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.service.OrganizationsServiceImpl
import com.hypto.iam.server.service.PolicyService
import com.hypto.iam.server.service.PolicyServiceImpl
import com.hypto.iam.server.service.ResourceService
import com.hypto.iam.server.service.ResourceServiceImpl
import com.hypto.iam.server.service.TokenService
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.service.UserPolicyService
import com.hypto.iam.server.service.UserPolicyServiceImpl
import com.hypto.iam.server.service.UsersService
import com.hypto.iam.server.service.UsersServiceImpl
import com.hypto.iam.server.service.ValidationService
import com.hypto.iam.server.service.ValidationServiceImpl
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IdGenerator
import com.hypto.iam.server.utils.policy.PolicyValidator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.bind
import org.koin.dsl.module
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient

// DI module to get repositories
val repositoryModule = module {
    single { MasterKeysRepo }
    single { CredentialsRepo }
    single { OrganizationRepo }
    single { PoliciesRepo }
    single { ResourceRepo }
    single { ActionRepo }
    single { UserAuthProvidersRepo }
    single { UserPoliciesRepo }
    single { UserRepo }
    single { HrnFactory }
}

val controllerModule = module {
    single { OrganizationsServiceImpl() } bind OrganizationsService::class
    single { TokenServiceImpl() } bind TokenService::class
    single { CredentialServiceImpl() } bind CredentialService::class
    single { PolicyServiceImpl() } bind PolicyService::class
    single { ValidationServiceImpl() } bind ValidationService::class
    single { UserPolicyServiceImpl() } bind UserPolicyService::class
    single { ResourceServiceImpl() } bind ResourceService::class
    single { ActionServiceImpl() } bind ActionService::class
    single { UsersServiceImpl() } bind UsersService::class
}

val applicationModule = module {
    single { Gson() }
    single { IdGenerator }
    single { ApplicationIdUtil.Generator }
    single { ApplicationIdUtil.Validator }
    single { PolicyValidator }
    single { AppConfig().configuration }
    single { CognitoIdentityProviderImpl() } bind IdentityProvider::class
    single { getCredentialsProvider(get<AppConfig.Config>().aws.accessKey,
        get<AppConfig.Config>().aws.secretKey) } bind AwsCredentialsProvider::class
    single { getCognitoIdentityProviderClient(get<AppConfig.Config>().aws.region, get()) }
}

fun getCognitoIdentityProviderClient(
    region: String,
    credentialsProvider: AwsCredentialsProvider
): CognitoIdentityProviderClient =
    CognitoIdentityProviderClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider).build()

fun getCredentialsProvider(accessKey: String, secretKey: String): StaticCredentialsProvider =
    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

/**
 * Used to inject a KoinComponent into a class / object as an extension.
 *
 * E.g: to inject gson into Policy model from some util class, do
 * val Policy.Companion.gson: Gson
 *    get() = getKoinInstance()
 *
 * This gson attribute can now be used in any other extension function of the Policy class
 */
inline fun <reified T> getKoinInstance(): T {
    return object : KoinComponent {
        val value: T by inject()
    }.value
}
