package com.hypto.iam.server

import com.codahale.metrics.Slf4jReporter
import com.hypto.iam.server.apis.*
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.infrastructure.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.metrics.dropwizard.DropwizardMetrics
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit
import org.koin.ktor.ext.Koin
import org.koin.logger.SLF4JLogger

@KtorExperimentalLocationsAPI
fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Koin) {
        SLF4JLogger()
        modules(repositoryModule)
    }
    install(DropwizardMetrics) {
        val reporter = Slf4jReporter.forRegistry(registry)
            .withLoggingLevel(Slf4jReporter.LoggingLevel.ERROR)
            .outputTo(log)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
        reporter.start(10, TimeUnit.SECONDS)
    }
    install(ContentNegotiation) {
        // TODO: Switch to kotlinx.serialization
        register(ContentType.Application.Json, GsonConverter())
    }

    install(StatusPages) {
        // TODO: Logic to update error message
    }
    install(AutoHeadResponse) // see http://ktor.io/features/autoheadresponse.html
    install(HSTS, ApplicationHstsConfiguration()) // see http://ktor.io/features/hsts.html
    install(Compression, ApplicationCompressionConfiguration()) // see http://ktor.io/features/compression.html
    install(Locations) // see http://ktor.io/features/locations.html
    install(Authentication) {
        apiKeyAuth("hypto-iam-root-auth") {
            validate { tokenCredential: TokenCredential ->
                when (tokenCredential.value) {
                    // TODO: Get secret key from db or cache
                    "hypto-root-secret-key" -> ApiPrincipal(tokenCredential, "hypto-root")
                    else -> null
                }
            }
        }
        bearer("bearer-auth") {
            validate { tokenCredential: TokenCredential ->
                when (tokenCredential.value) {
                    // TODO: Validate bearer token from db
                    "test-bearer-token" -> UserPrincipal(tokenCredential, "hypto", "ABC123")
                    else -> null
                }
            }
        }
    }

    install(Routing) {
        authenticate("hypto-iam-root-auth") {
            createOrganizationApi()
        }

        authenticate("bearer-auth") {
            organizationApi()
            actionApi()
            credentialApi()
            policyApi()
            resourceTypeApi()
            tokenApi()
            usersApi()
        }
    }
}

@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    embeddedServer(
        Netty, 8081, module = Application::module).start(wait = true)
}
