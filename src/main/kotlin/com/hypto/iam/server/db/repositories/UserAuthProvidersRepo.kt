package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.UsersAuthProviders
import com.hypto.iam.server.db.tables.records.UsersAuthProvidersRecord
import org.jooq.impl.DAOImpl

object UserAuthProvidersRepo : BaseRepo<UsersAuthProvidersRecord, UsersAuthProviders, Int>() {

    private val idFun = fun (usersAuthProviders: UsersAuthProviders): Int {
        return usersAuthProviders.id
    }

    override suspend fun dao(): DAOImpl<UsersAuthProvidersRecord, UsersAuthProviders, Int> {
        return txMan.getDao(
            com.hypto.iam.server.db.tables.UsersAuthProviders.USERS_AUTH_PROVIDERS,
            UsersAuthProviders::class.java,
            idFun
        )
    }
}
