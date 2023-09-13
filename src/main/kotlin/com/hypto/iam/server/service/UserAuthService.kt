package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.db.tables.records.UserAuthRecord
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.UserAuthMethod
import com.hypto.iam.server.models.UserAuthMethodsPaginatedResponse
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserAuthServiceImpl : KoinComponent, UserAuthService {
    private val organizationRepo: OrganizationRepo by inject()
    private val userAuthRepo: UserAuthRepo by inject()

    override suspend fun listUserAuth(
        organizationId: String,
        userId: String,
        paginationContext: PaginationContext
    ): UserAuthMethodsPaginatedResponse {
        organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userAuth = userAuthRepo.fetchUserAuthPaginated(
            ResourceHrn(organizationId, "", IamResources.USER, userId),
            paginationContext
        ).map {
            getUserAuthMethod(it)
        }.toList()
        val newContext = PaginationContext.from(userAuth.lastOrNull()?.providerName, paginationContext)

        return UserAuthMethodsPaginatedResponse(
            userAuth,
            newContext.nextToken,
            newContext.toOptions()
        )
    }

    private fun getUserAuthMethod(userAuth: UserAuthRecord): UserAuthMethod {
        return UserAuthMethod(
            providerName = userAuth.providerName
        )
    }
}

interface UserAuthService {
    suspend fun listUserAuth(
        organizationId: String,
        userHrn: String,
        paginationContext: PaginationContext
    ): UserAuthMethodsPaginatedResponse
}
