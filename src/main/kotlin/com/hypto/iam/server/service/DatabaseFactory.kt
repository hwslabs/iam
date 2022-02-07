package com.hypto.iam.server.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DatabaseFactory {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun connectAndMigrate() {
        log.info("Initialising database")
        val pool = hikari()
        Database.connect(pool)
        runFlyway(pool)
    }

    private fun hikari(): HikariDataSource {
        // TODO: Configure H2 database for unit tests
//        val config = HikariConfig().apply {
//            driverClassName = "org.h2.Driver"
//            jdbcUrl = "jdbc:h2:mem:test"
//            maximumPoolSize = 3
//            isAutoCommit = false
//            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
//            validate()
//        }

        // TODO: Read from config file instead of hardcoding
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5435/iam"
            maximumPoolSize = 3
            minimumIdle = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            username = "root"
            password = "password"
            validate()
        }


        return HikariDataSource(config)
    }

    private fun runFlyway(datasource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(datasource)
            .load()
        try {
            flyway.info()
            flyway.migrate()
        } catch (e: Exception) {
            log.error("Exception running flyway migration", e)
            throw e
        }
        log.info("Flyway migration has finished")
    }

    suspend fun <T> dbQuery(
        block: suspend () -> T
    ): T =
        newSuspendedTransaction { block() }

}