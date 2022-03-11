@file:Suppress("LongParameterList")
package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.util.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UsersServiceImpl : KoinComponent, UsersService {
    private val repo: UserRepo by inject()
    private val hrnFactory: HrnFactory by inject()
    override suspend fun createUser(
        organizationId: String,
        userName: String,
        password: String,
        email: String,
        userType: User.UserType,
        createdBy: String,
        status: User.Status,
        phone: String
    ): User {
        val userHrn = ResourceHrn(organizationId, "", IamResourceTypes.USER, userName)
        if (repo.existsById(userHrn.toString())) {
            throw EntityAlreadyExistsException("User name already exist")
        }

        // TODO: Remove this impl and use cognito
        val user = Users(userHrn.toString(),
            password,
            email,
            phone,
            true,
            userType.toString(),
            status.toString(),
            UUID.randomUUID(),
            organizationId, LocalDateTime.now(), LocalDateTime.now())
        repo.insert(user)
        return getUser(userHrn.toString())
    }

    override suspend fun getUser(hrn: String): User {
        val userRecord = repo.fetchByHrn(hrn) ?: throw EntityNotFoundException("User hrn $hrn not found")
        return User(
            id = userRecord.hrn,
            username = hrnFactory.getHrn(userRecord.hrn).toString(),
            organizationId = userRecord.organizationId,
            email = userRecord.email,
            phone = userRecord.phone,
            userType = User.UserType.valueOf(userRecord.userType),
            status = User.Status.valueOf(userRecord.status),
            loginAccess = userRecord.loginAccess,
            createdAt = userRecord.createdAt.toString(),
            createdBy = userRecord.createdBy.toString()
        )
    }

    override suspend fun updateUser(): User {
        TODO("Not yet implemented")
    }

    override suspend fun deleteUser(id: String) {
        TODO("Not yet implemented")
    }
}

/**
 * Service which holds logic related to Organization operations
 */
interface UsersService {
    suspend fun createUser(
        organizationId: String,
        userName: String,
        password: String,
        email: String,
        userType: User.UserType,
        createdBy: String,
        status: User.Status,
        phone: String
    ): User
    suspend fun getUser(id: String): User
    suspend fun updateUser(): User
    suspend fun deleteUser(id: String)
}
