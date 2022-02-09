package com.hypto.iam.server.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway

class DatabaseFactory {
    companion object {
        // TODO: Read from config file instead of hardcoding
        val pool: HikariDataSource = HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5435/iam"
            maximumPoolSize = 3
            minimumIdle = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            username = "root"
            password = "password"
        })
        val dataSource = HikariDataSource(pool)
    }

    private fun runFlyway(datasource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(datasource)
            .load()
        try {
            flyway.info()
            flyway.migrate()
        } catch (e: Exception) {
            throw e
        }
    }
}
