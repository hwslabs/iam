package com.hypto.iam.server

import com.hypto.iam.server.apis.OrganizationApi
import com.hypto.iam.server.db.models.tables.Organizations
import com.hypto.iam.server.service.DatabaseFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.impl.DSL
//import java.util.concurrent.Flow.Publisher

@KtorExperimentalLocationsAPI
fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    // TODO: Disable metrics for development mode
//    install(DropwizardMetrics) {
//        val reporter = Slf4jReporter.forRegistry(registry)
//            .outputTo(log)
//            .convertRatesTo(TimeUnit.SECONDS)
//            .convertDurationsTo(TimeUnit.MILLISECONDS)
//            .build()
//        reporter.start(10, TimeUnit.SECONDS)
//    }
    install(ContentNegotiation) {
        // TODO: Switch to kotlinx.serialization
        register(ContentType.Application.Json, GsonConverter())
    }

    install(AutoHeadResponse) // see http://ktor.io/features/autoheadresponse.html
    install(HSTS, ApplicationHstsConfiguration()) // see http://ktor.io/features/hsts.html
    install(Compression, ApplicationCompressionConfiguration()) // see http://ktor.io/features/compression.html
    install(Locations) // see http://ktor.io/features/locations.html

    // TODO: Add support for Bearer token authentication
//    install(Authentication) {}

//    val conn = DatabaseFactory.getConnection()
//    val create = DSL.using(conn, SQLDialect.POSTGRES)
//    val result: Result<Record> = create.select().from(Organizations()).fetch()

    install(Routing) {
//        ActionApi()
//        CredentialApi()
        OrganizationApi()
//        PolicyApi()
//        ResourceTypeApi()
//        TokenApi()
//        UsersApi()
    }
}

@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    embeddedServer(
        Netty, 8081, module = Application::module, watchPaths = listOf("classes", "resources")
    ).start(wait = true)
}


