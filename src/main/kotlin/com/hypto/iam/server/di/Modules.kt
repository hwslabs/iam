package com.hypto.iam.server.di

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.repositories.UserAuthProvidersRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
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
import com.hypto.iam.server.utils.IdGenerator
import com.hypto.iam.server.utils.policy.PolicyValidator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.bind
import org.koin.dsl.module

// DI module to get repositories
val repositoryModule = module {
    single { MasterKeysRepo }
    single { ActionRepo }
    single { CredentialsRepo }
    single { OrganizationRepo }
    single { PoliciesRepo }
    single { ResourceRepo }
    single { UserAuthProvidersRepo }
    single { UserPoliciesRepo }
    single { UserRepo }
}

val controllerModule = module {
    single { OrganizationsServiceImpl() } bind OrganizationsService::class
    single { TokenServiceImpl() } bind TokenService::class
    single { CredentialServiceImpl() } bind CredentialService::class
    single { PolicyServiceImpl() } bind PolicyService::class
    single { ValidationServiceImpl() } bind ValidationService::class
    single { UserPolicyServiceImpl() } bind UserPolicyService::class
    single { ResourceServiceImpl() } bind ResourceService::class
    single { UsersServiceImpl() } bind UsersService::class
}

val applicationModule = module {
    single { Gson() }
    single { IdGenerator }
    single { ApplicationIdUtil.Generator }
    single { ApplicationIdUtil.Validator }
    single { PolicyValidator }
}

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
