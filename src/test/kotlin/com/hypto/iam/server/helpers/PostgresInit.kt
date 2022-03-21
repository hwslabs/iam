package com.hypto.iam.server.helpers

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.testcontainers.containers.PostgreSQLContainer

/**
 * This object is used to initialize the testcontainers only once for all the tests.
 */
object PostgresInit {
    init {
        val testContainer =
            PostgreSQLContainer("postgres:14.1-alpine")
                .withDatabaseName("iam")
                .withUsername("root")
                .withPassword("password")

        testContainer.start()

        val configuration = ClassicConfiguration()
        configuration.setDataSource(
            testContainer.jdbcUrl,
            testContainer.username,
            testContainer.password
        )
        configuration.setLocations(Location("filesystem:src/main/resources/db/migration"))
        val flyway = Flyway(configuration)
        flyway.migrate()

        System.setProperty("config.override.database.name", "iam")
        System.setProperty("config.override.database.user", "root")
        System.setProperty("config.override.database.password", "password")
        System.setProperty("config.override.database.host", testContainer.host)
        System.setProperty(
            "config.override.database.port",
            testContainer.firstMappedPort.toString()
        )
    }
}
