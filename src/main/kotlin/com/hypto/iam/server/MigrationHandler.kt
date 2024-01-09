package com.hypto.iam.server

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.di.applicationModule
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.logger.SLF4JLogger
import java.util.TimeZone

class MigrationHandler : KoinComponent {
    private val appConfig: AppConfig by inject()

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

            startKoin {
                SLF4JLogger()
                modules(arrayListOf(applicationModule))
            }

            val migrationHandler = MigrationHandler()
            runBlocking { migrationHandler.migrate() }
        }
    }

    private fun migrate() {
        val flyway =
            Flyway.configure()
                .dataSource(
                    appConfig.database.jdbcUrl,
                    appConfig.database.username,
                    appConfig.database.password,
                ).load()
        flyway.migrate()
    }
}
