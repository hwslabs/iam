package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.UsersAuthProviders
import com.hypto.iam.server.db.tables.records.UsersAuthProvidersRecord
import org.jooq.impl.DAOImpl
import java.util.Optional
import java.util.UUID

object UserAuthProvidersRepo : DAOImpl<UsersAuthProvidersRecord, UsersAuthProviders, Int>(
    com.hypto.iam.server.db.tables.UsersAuthProviders.USERS_AUTH_PROVIDERS,
    UsersAuthProviders::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(usersAuthProviders: UsersAuthProviders): Int {
        return usersAuthProviders.id
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOneById(value: Int): UsersAuthProviders? {
        return fetchOne(com.hypto.iam.server.db.tables.UsersAuthProviders.USERS_AUTH_PROVIDERS.ID, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOptionalById(value: Int): Optional<UsersAuthProviders> {
        return fetchOptional(com.hypto.iam.server.db.tables.UsersAuthProviders.USERS_AUTH_PROVIDERS.ID, value)
    }

    /**
     * Fetch records that have `user_id = value`
     */
    fun fetchByUserId(value: UUID): List<UsersAuthProviders> {
        return fetch(com.hypto.iam.server.db.tables.UsersAuthProviders.USERS_AUTH_PROVIDERS.USER_ID, value)
    }
}
