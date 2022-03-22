package com.hypto.iam.server.configs

import com.sksamuel.hoplite.ConfigLoader

class AppConfig {
    /**
    Environment variables should be in Snake case.
    E.g. env, token_validity

    Nested variables should be defined with the data class name followed by 2 underscores and then the variable name
    {data_class_name}__{variable_name}
    E.g. For Database - database__host, database__port, database__username, database__password
     */

    data class Database(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val maximumPoolSize: Int,
        val minimumIdle: Int,
        val isAutoCommit: Boolean,
        val transactionIsolation: String
    ) {
        val jdbcUrl: String
            get() = "jdbc:postgresql://$host:$port/iam"
    }

    enum class Environment { Development, Staging, Production }

    /**
     * @param jwtTokenValidity Represents how long the JWT token must be valid from the instant of creation
     * @param oldKeyTtl Represents the TTL in seconds until which the rotated key must be available for
        verifying signatures (Default: 600s, 2X the local cache duration)
     * @param secretKey Internal key to create & delete organizations
     */
    data class App(
        val env: Environment,
        val jwtTokenValidity: Long,
        val oldKeyTtl: Long,
        val secretKey: String
    ) {
        val isDevelopment: Boolean
            get() = env == Environment.Development
    }

    // Get NewRelic licence key from https://one.newrelic.com/admin-portal/api-keys/home
    data class Newrelic(val apiKey: String, val publishInterval: Long)

    data class Aws(val region: String, val accessKey: String, val secretKey: String)
    data class Config(val app: App, val database: Database, val newrelic: Newrelic, val aws: Aws)
    val configuration: Config = ConfigLoader().loadConfigOrThrow("/default_config.json")
}
