package com.hypto.iam.server

import com.codahale.metrics.Slf4jReporter
import com.hypto.iam.server.apis.ActionApi
import com.hypto.iam.server.apis.CredentialApi
import com.hypto.iam.server.apis.OrganizationApi
import com.hypto.iam.server.apis.PolicyApi
import com.hypto.iam.server.apis.ResourceTypeApi
import com.hypto.iam.server.apis.TokenApi
import com.hypto.iam.server.apis.UsersApi
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.HSTS
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.metrics.dropwizard.DropwizardMetrics
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit

@KtorExperimentalLocationsAPI
fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(DropwizardMetrics) {
        val reporter = Slf4jReporter.forRegistry(registry)
//            .outputTo(log)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
        reporter.start(10, TimeUnit.SECONDS)
    }
    install(ContentNegotiation) {
        // TODO: Switch to kotlinx.serialization
        register(ContentType.Application.Json, GsonConverter())
    }

    install(AutoHeadResponse) // see http://ktor.io/features/autoheadresponse.html
    install(HSTS, ApplicationHstsConfiguration()) // see http://ktor.io/features/hsts.html
    install(Compression, ApplicationCompressionConfiguration()) // see http://ktor.io/features/compression.html
    install(Locations) // see http://ktor.io/features/locations.html
    install(Authentication) {
        // TODO: Add support for Bearer token authentication
    }

    install(Routing) {
        ActionApi()
        CredentialApi()
        OrganizationApi()
        PolicyApi()
        ResourceTypeApi()
        TokenApi()
        UsersApi()
    }
}

@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    embeddedServer(
        Netty, 8081, module = Application::module, watchPaths = listOf("classes", "resources")
    ).start(wait = true)
}
