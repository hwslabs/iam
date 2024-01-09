package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.AUTH_PROVIDER
import com.hypto.iam.server.db.tables.pojos.AuthProvider
import com.hypto.iam.server.db.tables.records.AuthProviderRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.impl.DAOImpl

typealias AuthProviderPk = Record1<String?>

object AuthProviderRepo : BaseRepo<AuthProviderRecord, AuthProvider, AuthProviderPk>() {
    @Suppress("ktlint:standard:blank-line-before-declaration")
    private fun getIdFun(dsl: DSLContext): (AuthProvider) -> AuthProviderPk {
        return fun (authProvider: AuthProvider): AuthProviderPk {
            return dsl.newRecord(
                AUTH_PROVIDER.PROVIDER_NAME,
            )
                .values(authProvider.providerName)
        }
    }

    override suspend fun dao(): DAOImpl<AuthProviderRecord, AuthProvider, AuthProviderPk> {
        return txMan.getDao(
            AUTH_PROVIDER,
            AuthProvider::class.java,
            getIdFun(txMan.dsl()),
        )
    }

    suspend fun fetchAuthProvidersPaginated(
        paginationContext: PaginationContext,
    ): List<AuthProviderRecord> {
        return ctx("authProvider.fetchAuthProvidersPaginated").selectFrom(AUTH_PROVIDER)
            .where()
            .paginate(AUTH_PROVIDER.PROVIDER_NAME, paginationContext)
            .fetch()
    }
}
