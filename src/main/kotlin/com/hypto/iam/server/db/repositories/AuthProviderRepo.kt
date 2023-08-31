package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.AUTH_PROVIDER
import com.hypto.iam.server.db.tables.pojos.AuthProvider
import com.hypto.iam.server.db.tables.records.AuthProviderRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.impl.DAOImpl

typealias AuthProviderPk = Record2<String?, String?>

object AuthProviderRepo : BaseRepo<AuthProviderRecord, AuthProvider, AuthProviderPk>() {

    private fun getIdFun(dsl: DSLContext): (AuthProvider) -> AuthProviderPk {
        return fun (authProvider: AuthProvider): AuthProviderPk {
            return dsl.newRecord(
                AUTH_PROVIDER.PROVIDER,
                AUTH_PROVIDER.GRANT_TYPE
            )
                .values(authProvider.provider, authProvider.grantType)
        }
    }

    override suspend fun dao(): DAOImpl<AuthProviderRecord, AuthProvider, AuthProviderPk> {
        return txMan.getDao(
            AUTH_PROVIDER,
            AuthProvider::class.java,
            getIdFun(txMan.dsl())
        )
    }

    suspend fun fetchAuthProvidersPaginated(
        paginationContext: PaginationContext
    ): List<AuthProviderRecord> {
        return ctx("authProvider.fetchAuthProvidersPaginated").selectFrom(AUTH_PROVIDER)
            .where()
            .paginate(AUTH_PROVIDER.CREATED_AT, paginationContext)
            .fetch()
    }
}
