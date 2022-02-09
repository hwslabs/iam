package com.hypto.iam.server.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.jooq.Configuration
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration

object DatabaseFactory {
    // TODO: Read from config file instead of hardcoding
    private val pool: HikariDataSource = HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://localhost:5435/iam"
        maximumPoolSize = 3
        minimumIdle = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        username = "root"
        password = "password"
    })

    private val daoConfiguration = DefaultConfiguration().derive(SQLDialect.POSTGRES).derive(pool)

    fun getConfiguration(): Configuration {
        return daoConfiguration
    }

    fun getConnection(): Connection {
        return pool.connection
    }
}
