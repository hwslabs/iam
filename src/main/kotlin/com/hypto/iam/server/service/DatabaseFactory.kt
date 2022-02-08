package com.hypto.iam.server.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class DatabaseFactory {
    companion object {
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
        var pool: HikariDataSource = HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5435/iam"
            maximumPoolSize = 3
            minimumIdle = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            username = "root"
            password = "password"
            validate()
        })

        fun getConnection(): Connection {
            return pool.connection
        }
    }
//    private val log = LoggerFactory.getLogger(this::class.java)
}