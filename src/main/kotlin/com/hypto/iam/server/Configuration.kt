package com.hypto.iam.server

// Use this file to hold package-level internal functions that return receiver object passed to the `install` method.

import com.hypto.iam.server.authProviders.AuthProviderRegistry
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.models.ResetPasswordRequest
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.UsernamePasswordCredential
import com.hypto.iam.server.security.apiKeyAuth
import com.hypto.iam.server.security.bearer
import com.hypto.iam.server.security.bearerAuthValidation
import com.hypto.iam.server.security.oauth
import com.hypto.iam.server.security.optionalBearer
import com.hypto.iam.server.security.passcodeAuth
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.service.PrincipalPolicyService
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.service.UserPrincipalService
import com.hypto.iam.server.validators.InviteMetadata
import com.hypto.iam.server.validators.validate
import com.newrelic.telemetry.micrometer.NewRelicRegistry
import com.newrelic.telemetry.micrometer.NewRelicRegistryConfig
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.basic
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.CompressionConfig
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.hsts.HSTSConfig
import io.ktor.server.request.receive
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.micrometer.core.instrument.util.NamedThreadFactory
import java.net.InetAddress
import java.time.Duration

/**
 * Application block for [HSTS] configuration.
 *
 * This file may be excluded in .openapi-generator-ignore,
 * and application specific configuration can be applied in this function.
 *
 * See http://ktor.io/features/hsts.html
 */
internal fun applicationHstsConfiguration(): HSTSConfig.() -> Unit {
    return {
        maxAgeInSeconds = Duration.ofDays(365).toMinutes() * 60
        includeSubDomains = true
        preload = false

        // You may also apply any custom directives supported by specific user-agent. For example:
        // customDirectives.put("redirectHttpToHttps", "false")
    }
}

/**
 * Application block for [Compression] configuration.
 *
 * This file may be excluded in .openapi-generator-ignore,
 * and application specific configuration can be applied in this function.
 *
 * See http://ktor.io/features/compression.html
 */
internal fun applicationCompressionConfiguration(): CompressionConfig.() -> Unit {
    return {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
}

@Suppress("ThrowsCount")
internal fun applicationAuthenticationConfiguration(): AuthenticationConfig.() -> Unit = {
    val appConfig = getKoinInstance<AppConfig>()
    val passcodeRepo = getKoinInstance<PasscodeRepo>()
    val passcodeService = getKoinInstance<PasscodeService>()
    val principalPolicyService = getKoinInstance<PrincipalPolicyService>()
    val userPrincipalService = getKoinInstance<UserPrincipalService>()

    oauth("oauth") {
        validate {
            val issuer = this.request.headers["x-issuer"] ?: throw BadRequestException("x-issuer header not found")
            AuthProviderRegistry.getProvider(issuer)?.getProfileDetails(it)
                ?: throw BadRequestException("No auth provider found for issuer $issuer")
        }
    }
    apiKeyAuth("hypto-iam-root-auth") {
        val secretKey = Constants.SECRET_PREFIX + appConfig.app.secretKey
        validate { tokenCredential: TokenCredential ->
            when (tokenCredential.value) {
                secretKey -> ApiPrincipal(tokenCredential, ROOT_ORG, TokenServiceImpl.ISSUER)
                else -> null
            }
        }
    }
    passcodeAuth("signup-passcode-auth") {
        validate { tokenCredential: TokenCredential ->
            tokenCredential.value?.let {
                passcodeRepo.getValidPasscodeById(it, VerifyEmailRequest.Purpose.signup)?.let {
                    ApiPrincipal(tokenCredential, ROOT_ORG, TokenServiceImpl.ISSUER)
                }
            }
        }
    }
    passcodeAuth("reset-passcode-auth") {
        validate { tokenCredential: TokenCredential ->
            val email = this.receive<ResetPasswordRequest>().validate().email
            tokenCredential.value?.let {
                passcodeRepo.getValidPasscodeById(it, VerifyEmailRequest.Purpose.reset, email)?.let {
                    ApiPrincipal(tokenCredential, ROOT_ORG, TokenServiceImpl.ISSUER)
                }
            }
        }
    }
    passcodeAuth("invite-passcode-auth") {
        validate { tokenCredential: TokenCredential ->
            tokenCredential.value?.let { value ->
                passcodeRepo.getValidPasscodeById(value, VerifyEmailRequest.Purpose.invite)?.let {
                    val metadata = InviteMetadata(passcodeService.decryptMetadata(it.metadata!!))
                    return@validate UserPrincipal(
                        tokenCredential = TokenCredential(tokenCredential.value, TokenType.PASSCODE),
                        issuer = TokenServiceImpl.ISSUER,
                        hrnStr = metadata.inviterUserHrn,
                        policies = principalPolicyService.fetchEntitlements(metadata.inviterUserHrn)
                    )
                }
            }
        }
    }
    basic("basic-auth") {
        validate { credentials ->
            val organizationId = this.parameters["organization_id"]!!
            val principal = userPrincipalService.getUserPrincipalByCredentials(
                organizationId,
                credentials.name.lowercase(),
                credentials.password,
                TokenServiceImpl.ISSUER
            )
            if (principal != null) {
                response.headers.append(Constants.X_ORGANIZATION_HEADER, organizationId)
            }
            return@validate principal
        }
    }
    bearer("bearer-auth") {
        validate(bearerAuthValidation(userPrincipalService))
    }
    basic("unique-basic-auth") {
        validate { credentials ->
            if (!appConfig.app.uniqueUsersAcrossOrganizations) {
                throw BadRequestException(
                    "Email not unique across organizations. " +
                        "Please use Token APIs with organization ID"
                )
            }

            val principal = userPrincipalService.getUserPrincipalByCredentials(
                UsernamePasswordCredential(credentials.name.lowercase(), credentials.password),
                TokenServiceImpl.ISSUER
            )
            response.headers.append(Constants.X_ORGANIZATION_HEADER, principal.organization)
            return@validate principal
        }
    }
    optionalBearer("optional-bearer-auth") {
        validate(bearerAuthValidation(userPrincipalService))
    }
}

// Defines authentication mechanisms used throughout the application.
val ApplicationAuthProviders: Map<String, OAuthServerSettings> = listOf<OAuthServerSettings>(
//        OAuthServerSettings.OAuth2ServerSettings(
//                name = "facebook",
//                authorizeUrl = "https://graph.facebook.com/oauth/authorize",
//                accessTokenUrl = "https://graph.facebook.com/oauth/access_token",
//                requestMethod = HttpMethod.Post,
//
//                clientId = "settings.property("auth.oauth.facebook.clientId").getString()",
//                clientSecret = "settings.property("auth.oauth.facebook.clientSecret").getString()",
//                defaultScopes = listOf("public_profile")
//        )
).associateBy { it.name }

// Provides all resources and configurations for application telemetry using micrometer
object MicrometerConfigs {
    private val appConfig = getKoinInstance<AppConfig>()
    private val registry = CompositeMeterRegistry()

    // TODO: Retain required metric binders and remove the rest
    private val meterBinders = listOf(
//        ClassLoaderMetrics(),
//        JvmMemoryMetrics(),
//        JvmGcMetrics(),
//        JvmThreadMetrics(),
//        JvmHeapPressureMetrics(),
        ProcessorMetrics(),
        LogbackMetrics()
    )

    private fun getNewRelicMeterRegistry(): NewRelicRegistry {
        val newRelicRegistry = NewRelicRegistry.builder(getNewRelicRegistryConfig()).build()
        newRelicRegistry.config() // TODO: Fix the config params as required
            .meterFilter(MeterFilter.ignoreTags("plz_ignore_me"))
            .meterFilter(MeterFilter.denyNameStartsWith("jvm.threads"))
        newRelicRegistry.start(NamedThreadFactory("newrelic.micrometer.registry"))
        return newRelicRegistry
    }

    private fun getNewRelicRegistryConfig(): NewRelicRegistryConfig {
        return object : NewRelicRegistryConfig {
            override fun apiKey() = appConfig.newrelic.apiKey

            override fun get(key: String): String? { return null }
            override fun step(): Duration {
                // TODO: Needs Tweaking
                return Duration.ofSeconds(appConfig.newrelic.publishInterval)
            }
            override fun serviceName() = "${appConfig.app.name}.${appConfig.app.env}.${appConfig.app.stack}"
            override fun enableAuditMode() = false
            override fun useLicenseKey() = true
        }
    }

    init {
        registry.config().commonTags(
            // TODO: Add instance_id, etc. as a common tag
            listOf(
                // Commenting below tags as this info is available in serviceName.
//                Tag.of("environment", appConfig.app.env.toString()),
//                Tag.of("service", appConfig.app.name),
//                Tag.of("stack", appConfig.app.stack)
                Tag.of("host", InetAddress.getLocalHost().hostName)
            )
        )
        /*
         * TODO: Configure "LoggingMeterRegistry" with a logging sink to
         *  direct metrics logs to a separate "togai_core_metrics.log" logback appender
         * http://javadox.com/io.micrometer/micrometer-core/1.2.1/io/micrometer/core/instrument/logging/LoggingMeterRegistry.Builder.html#loggingSink(java.util.function.Consumer)
         */
        if (appConfig.app.isDevelopment) {
            registry.add(LoggingMeterRegistry())
        } else {
            registry.add(getNewRelicMeterRegistry())
        }
    }

    fun getRegistry(): MeterRegistry {
        return registry
    }

    fun getBinders(): List<MeterBinder> {
        return meterBinders
    }
}
