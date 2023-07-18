package com.hypto.iam.server.configs

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource

data class AppConfig(val app: App, val server: Server, val database: Database, val newrelic: Newrelic, val aws: Aws) {
    /**
     * Environment variables should be in Snake case.
     * E.g. env, token_validity
     *
     * Nested variables should be defined with the data class name followed by 2 underscores and then the variable name
     * {data_class_name}__{variable_name}
     * E.g. For Database - database__host, database__port, database__username, database__password
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
     *                    verifying signatures (Default: 600s, 2X the local cache duration)
     * @param secretKey Internal key to create & delete organizations
     * @param signKeyFetchInterval Represents how frequently the private key
     *                                  for signing JWT tokens must be fetched from DB
     * @param cacheRefreshInterval Represents how frequently the local Key cache for
     *                                  verifying tokens must be refreshed
     * @param passcodeValiditySeconds Represents how long the passcode must be valid in seconds
     * @param passcodeCountLimit Represents the maximum number of passcodes that can be generated for a email
     * @param baseUrl Url used for sending links in emails
     * @param senderEmailAddress Email address used for sending emails
     * @param signUpEmailTemplate Template name used for sending emails for verification
     *                              during signup
     * @param resetPasswordEmailTemplate Template name used for sending emails during resetting password
     * @param uniqueUsersAcrossOrganizations Represents whether the users should be unique across all
     *                                          organizations. Uniqueness is determined with the user emails.
     * @param strictPolicyStatementValidation Decides whether to validate policy statements strictly by only
     *                                          accepting resource and action names present in resources & actions
     *                                          tables. This will also accept "*" but won't accept any other Regex.
     */
    data class App(
        val env: Environment,
        val name: String,
        val stack: String,
        val jwtTokenValidity: Long,
        val oldKeyTtl: Long,
        val secretKey: String,
        val signKeyFetchInterval: Long,
        val cacheRefreshInterval: Long,
        val passcodeValiditySeconds: Long,
        val passcodeCountLimit: Long,
        val baseUrl: String,
        val senderEmailAddress: String,
        val signUpEmailTemplate: String,
        val inviteUserEmailTemplate: String,
        val resetPasswordEmailTemplate: String,
        val uniqueUsersAcrossOrganizations: Boolean,
        val strictPolicyStatementValidation: Boolean
    ) {
        val isDevelopment: Boolean
            get() = env == Environment.Development
    }

    /**
     * @param port Specifies the port to which the application should be bound to.
     * {@code io.ktor.server.engine.ApplicationEngine.Configuration}
     * @param connectionGroupSize Specifies size of the event group for accepting connections
     * @param workerGroupSize Specifies size of the event group for processing connections,
     *                          parsing messages and doing engine's internal work
     * @param callGroupSize Specifies size of the event group for running application code
     */
    @Suppress("MagicNumber")
    data class Server(
        // Ktor
        val port: Int = 8080,
        val connectionGroupSize: Int = Runtime.getRuntime().availableProcessors() * 2,
        val workerGroupSize: Int = Runtime.getRuntime().availableProcessors() * 4,
        val callGroupSize: Int = Runtime.getRuntime().availableProcessors() * 16,
    )

    // Get NewRelic licence key from https://one.newrelic.com/admin-portal/api-keys/home
    data class Newrelic(val apiKey: String, val publishInterval: Long)

    data class Aws(val region: String, val accessKey: String, val secretKey: String)

    companion object {
        val configuration: AppConfig = ConfigLoaderBuilder.default()
            .addPropertySource(
                EnvironmentVariablesPropertySource(useUnderscoresAsSeparator = true, allowUppercaseNames = true)
            ).withReport().build().loadConfigOrThrow("/default_config.json")
    }
}
