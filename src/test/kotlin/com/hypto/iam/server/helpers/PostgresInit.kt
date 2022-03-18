package com.hypto.iam.server.helpers

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.testcontainers.containers.PostgreSQLContainer

object PostgresInit {
    init {
        val container =
            PostgreSQLContainer("postgres:14.1-alpine")
                .withDatabaseName("iam")
                .withUsername("root")
                .withPassword("password")

        container.start()

        val configuration = ClassicConfiguration()
        configuration.setDataSource(
            container.jdbcUrl,
            container.username,
            container.password
        )
        configuration.setLocations(Location("filesystem:src/main/resources/db/migration"))
        val flyway = Flyway(configuration)
        flyway.migrate()

        System.setProperty("config.override.database.name", "iam")
        System.setProperty("config.override.database.user", "root")
        System.setProperty("config.override.database.password", "password")
        System.setProperty("config.override.database.host", container.host)
        System.setProperty(
            "config.override.database.port",
            container.firstMappedPort.toString()
        )
    }
}
