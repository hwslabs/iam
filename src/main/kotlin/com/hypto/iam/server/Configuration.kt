package com.hypto.iam.server

// Use this file to hold package-level internal functions that return receiver object passed to the `install` method.

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.di.getKoinInstance
import com.newrelic.telemetry.micrometer.NewRelicRegistry
import com.newrelic.telemetry.micrometer.NewRelicRegistryConfig
import io.ktor.auth.OAuthServerSettings
import io.ktor.features.Compression
import io.ktor.features.HSTS
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.minimumSize
import io.ktor.util.KtorExperimentalAPI
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
import java.util.concurrent.Executors

/**
 * Application block for [HSTS] configuration.
 *
 * This file may be excluded in .openapi-generator-ignore,
 * and application specific configuration can be applied in this function.
 *
 * See http://ktor.io/features/hsts.html
 */
internal fun applicationHstsConfiguration(): HSTS.Configuration.() -> Unit {
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
internal fun applicationCompressionConfiguration(): Compression.Configuration.() -> Unit {
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

// Defines authentication mechanisms used throughout the application.
@KtorExperimentalAPI
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

// Provides an application-level fixed thread pool on which to execute coroutines (mainly)
internal val ApplicationExecutors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4)

// Provides all resources and configurations for application telemetry using micrometer
object MicrometerConfigs {
    private val registry = CompositeMeterRegistry()
//        .add(getNewRelicMeterRegistry()) // TODO: Uncomment this to publish metrics to new relic

        /*
         * TODO: Configure "LoggingMeterRegistry" with a logging sink to direct metrics logs to a separate "iam_metrics.log" logback appender
         * http://javadox.com/io.micrometer/micrometer-core/1.2.1/io/micrometer/core/instrument/logging/LoggingMeterRegistry.Builder.html#loggingSink(java.util.function.Consumer)
         */
        .add(LoggingMeterRegistry())

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
        val newRelicRegistry = NewRelicRegistry.builder(getNewRelicRegistryConfig())
            .commonAttributes(
                com.newrelic.telemetry.Attributes()
                    .put("host", InetAddress.getLocalHost().hostName)
            )
            .build()
        newRelicRegistry.config() // TODO: Fix the config params as required
            .meterFilter(MeterFilter.ignoreTags("plz_ignore_me"))
            .meterFilter(MeterFilter.denyNameStartsWith("jvm.threads"))
        newRelicRegistry.start(NamedThreadFactory("newrelic.micrometer.registry"))
        return newRelicRegistry
    }

    private fun getNewRelicRegistryConfig(): NewRelicRegistryConfig {
        return object : NewRelicRegistryConfig {
            val appConfig = getKoinInstance<AppConfig>()

            override fun apiKey(): String {
                return appConfig.configuration.newrelic.apiKey
            }

            override fun get(key: String): String? { return null }
            override fun step(): Duration {
                // TODO: Needs Tweaking
                return Duration.ofSeconds(appConfig.configuration.newrelic.publishInterval)
            }
            override fun serviceName(): String { return "Hypto IAM - " + appConfig.configuration.app.env }
            override fun enableAuditMode(): Boolean { return false }
            override fun useLicenseKey(): Boolean { return true }
        }
    }

    init {
        registry.config().commonTags(
            listOf(Tag.of("environment", getKoinInstance<AppConfig>().configuration.app.env))
        )
    }

    fun getRegistry(): MeterRegistry {
        return registry
    }

    fun getBinders(): List<MeterBinder> {
        return meterBinders
    }
}
