package com.hypto.iam.server.helpers

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.extensions.usersFrom
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID

class MockStore {
    val organizationIdMap = mutableMapOf<String, Organizations>()
    val userIdMap = mutableMapOf<String, UsersRecord>()
    val credentialTokenMap = mutableMapOf<String, CredentialsRecord>()
    val credentialIdMap = mutableMapOf<String, CredentialsRecord>()
    val policyIdMap = mutableMapOf<String, PoliciesRecord>()
    val userToPolicyMap = mutableMapOf<String /*User HRN*/, MutableList<String> /*List of Policy HRNs*/>()
    val policyToUserMap = mutableMapOf<String /*Policy HRN*/, String /*USer HRN*/>()

    fun clear() {
        organizationIdMap.clear()
        userIdMap.clear()
        credentialTokenMap.clear()
        credentialIdMap.clear()
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
        val user = UsersRecord()
            .setHrn(hrnString)
            .setCreatedAt(LocalDateTime.MAX)
            .setUpdatedAt(LocalDateTime.MAX)
            .setStatus(User.Status.active.value)
            .setUserType(User.UserType.normal.value)
            .setEmail("testEmail")
            .setPasswordHash("testSaltedPassword")
        store.userIdMap[hrnString] = user
        return usersFrom(user)
    }

    fun mockFetchByHrn(userRepo: UserRepo) {
        coEvery { userRepo.fetchByHrn(any()) } coAnswers {
            store.userIdMap[firstArg()]
        }
    }

    fun mockExistsById(userRepo: UserRepo) {
        coEvery { userRepo.existsById(any()) } coAnswers {
            store.userIdMap.containsKey(firstArg())
        }
    }

    fun mockCreate(userRepo: UserRepo) {
        coEvery {
            userRepo.create(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            val record = UsersRecord()
                .setHrn(firstArg<Hrn>().toString())
                .setPasswordHash(secondArg())
                .setEmail(thirdArg())
                .setPhone(arg(3))
                .setLoginAccess(true)
                .setUserType(arg<User.UserType>(4).value)
                .setStatus(arg<User.Status>(5).value)
                .setCreatedBy(arg<Hrn>(6).toString())
                .setOrganizationId(firstArg<Hrn>().organization)
                .setCreatedAt(arg(7))
                .setUpdatedAt(arg(8))
            store.userIdMap[firstArg<Hrn>().toString()] = record
            record
        }
    }
}

class MockCredentialsStore(private val store: MockStore) {
    private fun insertCredential(cred: CredentialsRecord) {
        store.credentialIdMap[cred.id.toString()] = cred
        store.credentialTokenMap[cred.refreshToken] = cred
    }

    private fun deleteCredential(identifier: String): Boolean {
        store.credentialIdMap.remove(identifier)
        return store.credentialTokenMap.remove(identifier) != null
    }

    // TODO: Replace this method once createOrganization API inherently creates user and credentials
    fun createCredential(userHrn: String): CredentialsRecord {
        val refreshToken = UUID.randomUUID()
        val credentialRecord = CredentialsRecord()
            .setId(refreshToken)
            .setStatus("active")
            .setUserHrn(userHrn)
            .setRefreshToken(refreshToken.toString())
        insertCredential(credentialRecord)
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
            insertCredential(credentialRecord)
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
            deleteCredential(thirdArg<UUID>().toString())
        }
    }

    fun mockFetchByIdAndUserHrn(credentialsRepo: CredentialsRepo) {
        coEvery { credentialsRepo.fetchByIdAndUserHrn(any(), any()) } coAnswers {
            store.credentialIdMap[firstArg<UUID>().toString()]
        }
    }
}

class MockPoliciesStore(private val store: MockStore) {
    fun mockCreate(policiesRepo: PoliciesRepo) {
        coEvery { policiesRepo.create(any(), any()) } coAnswers {
            val record = PoliciesRecord()
                .setHrn(firstArg<ResourceHrn>().toString())
                .setOrganizationId(firstArg<ResourceHrn>().organization)
                .setVersion(1)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .setStatements(secondArg())
            store.policyIdMap[record.hrn] = record
            record
        }
    }

    fun mockExistsByIds(policiesRepo: PoliciesRepo) {
        coEvery { policiesRepo.existsByIds(any()) } coAnswers {
            firstArg<List<String>>().all { store.policyIdMap.containsKey(it) }
        }
    }

    fun mockExistsById(policiesRepo: PoliciesRepo) {
        coEvery { policiesRepo.existsById(any()) } coAnswers {
            store.policyIdMap.containsKey(firstArg())
        }
    }
}

class MockUserPoliciesStore(private val store: MockStore) {
    fun mockInsert(userPoliciesRepo: UserPoliciesRepo) {
        coEvery { userPoliciesRepo.insert(any<List<UserPoliciesRecord>>()) } coAnswers {
            firstArg<List<UserPoliciesRecord>>().forEach {
                if (store.userToPolicyMap.containsKey(it.principalHrn)) {
                    store.userToPolicyMap[it.principalHrn]!!.add(it.policyHrn)
                } else {
                    store.userToPolicyMap[it.principalHrn] = mutableListOf(it.policyHrn)
                }
                store.policyToUserMap[it.policyHrn] = it.principalHrn
            }

            mockk()
        }
    }
}
