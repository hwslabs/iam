package com.hypto.iam.server.di

import com.google.gson.Gson
import com.hypto.iam.server.controller.OrganizationsService
import com.hypto.iam.server.controller.OrganizationsServiceImpl
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceTypeRepo
import com.hypto.iam.server.db.repositories.UserAuthProvidersRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.utils.IdUtil
import org.koin.dsl.bind
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

val controllerModule = module {
    single { OrganizationsServiceImpl() } bind OrganizationsService::class
}

val applicationModule = module {
    single { Gson() }
    single { IdUtil }
}
