package com.hypto.iam.server.service

import com.hypto.iam.server.db.listeners.DeleteOrUpdateWithoutWhereListener
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.jooq.Configuration
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider

object DatabaseFactory {
    // TODO: Read from config file instead of hardcoding
    private val pool: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5435/iam"
            maximumPoolSize = 3
            minimumIdle = 3
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            username = "root"
            password = "password"
        }
    )

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
