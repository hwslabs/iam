package com.hypto.iam.server.service

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.listeners.DeleteOrUpdateWithoutWhereListener
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.jooq.Configuration
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object DatabaseFactory : KoinComponent {
    private val appConfig: AppConfig by inject()

    private val pool: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = appConfig.database.jdbcUrl
            maximumPoolSize = appConfig.database.maximumPoolSize
            minimumIdle = appConfig.database.minimumIdle
            isAutoCommit = appConfig.database.isAutoCommit
            transactionIsolation = appConfig.database.transactionIsolation
            username = appConfig.database.username
            password = appConfig.database.password
        }
    )

    init {
        // eager initializing connection pools
        pool.connection.prepareStatement("select 'HELLO_WORLD';").execute()
    }

    fun init() { /* Deliberately left empty*/
    }

    private val daoConfiguration = DefaultConfiguration()
        .set(SQLDialect.POSTGRES)
        .set(pool)
        .deriveSettings {
            // https://www.jooq.org/doc/latest/manual/sql-building/dsl-context/custom-settings/settings-return-all-on-store/
            it.withReturnAllOnUpdatableRecord(true)
        }.setAppending(DefaultExecuteListenerProvider(DeleteOrUpdateWithoutWhereListener()))

    fun getConfiguration(): Configuration {
        return daoConfiguration
    }

    fun getConnection(): Connection {
        return pool.connection
    }
}
