package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.models.UserAuthMethod
import com.hypto.iam.server.models.UserAuthMethodsResponse
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserAuthServiceImpl : KoinComponent, UserAuthService {
    private val organizationRepo: OrganizationRepo by inject()
    private val userAuthRepo: UserAuthRepo by inject()

    override suspend fun listUserAuth(
        organizationId: String,
        userId: String
    ): UserAuthMethodsResponse {
        organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userAuth = userAuthRepo.fetchUserAuth(
            ResourceHrn(organizationId, "", IamResources.USER, userId)
        ).map {
            UserAuthMethod(
                providerName = it.providerName
            )
        }.toList()

        return UserAuthMethodsResponse(userAuth)
    }
}

interface UserAuthService {
    suspend fun listUserAuth(
        organizationId: String,
        userHrn: String
    ): UserAuthMethodsResponse
}
