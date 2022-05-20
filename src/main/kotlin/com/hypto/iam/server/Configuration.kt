package com.hypto.iam.server

// Use this file to hold package-level internal functions that return receiver object passed to the `install` method.

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.di.getKoinInstance
import com.newrelic.telemetry.micrometer.NewRelicRegistry
import com.newrelic.telemetry.micrometer.NewRelicRegistryConfig
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.CompressionConfig
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.hsts.HSTSConfig
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
            override fun apiKey(): String {
                return appConfig.newrelic.apiKey
            }

            override fun get(key: String): String? { return null }
            override fun step(): Duration {
                // TODO: Needs Tweaking
                return Duration.ofSeconds(appConfig.newrelic.publishInterval)
            }
            // TODO: [IMPORTANT] Read service name from appConfig
            override fun serviceName(): String { return "Hypto IAM - " + appConfig.app.env }
            override fun enableAuditMode(): Boolean { return false }
            override fun useLicenseKey(): Boolean { return true }
        }
    }

    init {
        /*
         * TODO: Configure "LoggingMeterRegistry" with a logging sink to
         *  direct metrics logs to a separate "togai_core_metrics.log" logback appender
         * http://javadox.com/io.micrometer/micrometer-core/1.2.1/io/micrometer/core/instrument/logging/LoggingMeterRegistry.Builder.html#loggingSink(java.util.function.Consumer)
         */
        if (!appConfig.app.isDevelopment) registry.add(LoggingMeterRegistry())
        // TODO: Uncomment this to publish metrics to new relic
        // registry.add(getNewRelicMeterRegistry())

        registry.config().commonTags(
            listOf(Tag.of("environment", appConfig.app.env.toString()))
        )
    }

    fun getRegistry(): MeterRegistry {
        return registry
    }

    fun getBinders(): List<MeterBinder> {
        return meterBinders
    }
}
