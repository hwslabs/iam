package com.hypto.iam.server.di

import com.hypto.iam.server.db.repositories.*
import org.koin.dsl.module

// DI module to get repositories
val repositoryModule = module {
    single { ActionRepo }
    single { CredentialsRepo }
    single { OrganizationRepo }
    single { PoliciesRepo }
    single { ResourceTypeRepo }
    single { UserAuthProvidersRepo }
    single { UserPoliciesRepo }
    single { UserRepo }
}
