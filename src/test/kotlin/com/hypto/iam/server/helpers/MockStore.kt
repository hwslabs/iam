package com.hypto.iam.server.helpers

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.extensions.usersFrom
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import io.mockk.coEvery
import java.time.LocalDateTime
import java.util.UUID

class MockStore {
    val organizationIdMap = mutableMapOf<String, Organizations>()
    val userIdMap = mutableMapOf<String, Users>()
    val credentialTokenMap = mutableMapOf<String, CredentialsRecord>()
    val credentialIdMap = mutableMapOf<String, CredentialsRecord>()

    fun clear() {
        organizationIdMap.clear()
        userIdMap.clear()
        credentialTokenMap.clear()
        credentialIdMap.clear()
    }

    fun insertCredential(cred: CredentialsRecord) {
        credentialIdMap[cred.id.toString()] = cred
        credentialTokenMap[cred.refreshToken] = cred
    }

    fun deleteCredential(identifier: String): Boolean {
        credentialIdMap.remove(identifier)
        return credentialTokenMap.remove(identifier) != null
    }
}

class MockOrganizationStore(private val store: MockStore) {
    fun mockInsert(orgRepo: OrganizationRepo) {
        coEvery { orgRepo.insert(any<Organizations>()) } coAnswers {
            val org = firstArg<Organizations>()
            store.organizationIdMap[org.id] = org
        }
    }

    fun mockFindById(orgRepo: OrganizationRepo, organizations: Organizations? = null) {
        coEvery { orgRepo.findById(any()) } coAnswers {
            organizations ?: store.organizationIdMap[firstArg()]
        }
    }
}

class MockUserStore(private val store: MockStore) {
    // TODO: Replace this method once createUser API is implemented
    fun createUser(organizationId: String, userName: String): Users {
        val hrnString = ResourceHrn(organizationId, null, IamResourceTypes.USER, userName).toString()
        val user = usersFrom(
            UsersRecord()
                .setHrn(hrnString)
                .setCreatedAt(LocalDateTime.MAX)
                .setUpdatedAt(LocalDateTime.MAX)
                .setStatus(User.Status.active.value)
                .setUserType(User.UserType.normal.value)
                .setEmail("testEmail")
                .setPasswordHash("testSaltedPassword")
        )
        store.userIdMap[hrnString] = user
        return user
    }

    fun mockFetchByHrn(userRepo: UserRepo) {
        coEvery { userRepo.fetchByHrn(any()) } coAnswers {
            store.userIdMap[firstArg()]
        }
    }
}

class MockCredentialsStore(private val store: MockStore) {
    // TODO: Replace this method once createOrganization API inherently creates user and credentials
    fun createCredential(userHrn: String): CredentialsRecord {
        val refreshToken = UUID.randomUUID()
        val credentialRecord = CredentialsRecord()
            .setId(refreshToken)
            .setStatus("active")
            .setUserHrn(userHrn)
            .setRefreshToken(refreshToken.toString())
        store.insertCredential(credentialRecord)
        return credentialRecord
    }

    fun mockCreate(credentialsRepo: CredentialsRepo) {
        coEvery { credentialsRepo.create(any(), any(), any(), any()) } coAnswers {
            val credentialRecord = CredentialsRecord(
                UUID.randomUUID(),
                lastArg(),
                secondArg<Credential.Status>().toString(),
                thirdArg(),
                firstArg<Hrn>().toString(),
                LocalDateTime.MAX, LocalDateTime.MAX
            )
            store.insertCredential(credentialRecord)
            credentialRecord
        }
    }

    fun mockFetchByRefreshToken(credentialsRepo: CredentialsRepo) {
        coEvery { credentialsRepo.fetchByRefreshToken(any()) } coAnswers {
            store.credentialTokenMap[firstArg()]
        }
    }

    fun mockDelete(credentialsRepo: CredentialsRepo) {
        coEvery { credentialsRepo.delete(any(), any(), any<UUID>()) } coAnswers {
            store.deleteCredential(thirdArg<UUID>().toString())
        }
    }

    fun mockFetchByIdAndUserHrn(credentialsRepo: CredentialsRepo) {
        coEvery { credentialsRepo.fetchByIdAndUserHrn(any(), any()) } coAnswers {
            store.credentialIdMap[firstArg<UUID>().toString()]
        }
    }
}
