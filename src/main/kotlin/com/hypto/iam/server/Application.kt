package com.hypto.iam.server

import com.codahale.metrics.Slf4jReporter
import com.hypto.iam.server.apis.actionApi
import com.hypto.iam.server.apis.createAndDeleteOrganizationApi
import com.hypto.iam.server.apis.credentialApi
import com.hypto.iam.server.apis.getAndUpdateOrganizationApi
import com.hypto.iam.server.apis.policyApi
import com.hypto.iam.server.apis.resourceTypeApi
import com.hypto.iam.server.apis.tokenApi
import com.hypto.iam.server.apis.usersApi
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.infrastructure.ApiPrincipal
import com.hypto.iam.server.infrastructure.TokenCredential
import com.hypto.iam.server.infrastructure.UserPrincipal
import com.hypto.iam.server.infrastructure.apiKeyAuth
import com.hypto.iam.server.infrastructure.bearer
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.HSTS
import io.ktor.features.StatusPages
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.metrics.dropwizard.DropwizardMetrics
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.ext.Koin
import org.koin.logger.SLF4JLogger
import java.security.Security
import java.util.concurrent.TimeUnit

@KtorExperimentalLocationsAPI
fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Koin) {
        SLF4JLogger()
        modules(repositoryModule, controllerModule, applicationModule)
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
    install(HSTS, applicationHstsConfiguration()) // see http://ktor.io/features/hsts.html
    install(Compression, applicationCompressionConfiguration()) // see http://ktor.io/features/compression.html
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
            createAndDeleteOrganizationApi()
        }

        authenticate("bearer-auth") {
            getAndUpdateOrganizationApi()
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
    // https://www.baeldung.com/java-bouncy-castle#setup-unlimited-strength-jurisdiction-policy-files
    Security.setProperty("crypto.policy", "unlimited")

    embeddedServer(
        Netty, 8081, module = Application::module).start(wait = true)
}
