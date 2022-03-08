@file:Suppress("LongMethod")
package com.hypto.iam.server

import com.hypto.iam.server.apis.actionApi
import com.hypto.iam.server.apis.createAndDeleteOrganizationApi
import com.hypto.iam.server.apis.credentialApi
import com.hypto.iam.server.apis.getAndUpdateOrganizationApi
import com.hypto.iam.server.apis.policyApi
import com.hypto.iam.server.apis.resourceTypeApi
import com.hypto.iam.server.apis.tokenApi
import com.hypto.iam.server.apis.usersApi
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.features.globalcalldata.GlobalCallData
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.Audit
import com.hypto.iam.server.security.Authorization
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.apiKeyAuth
import com.hypto.iam.server.security.bearer
import com.hypto.iam.server.utils.ApplicationIdUtil
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.HSTS
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.security.Security
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.SLF4JLogger

private const val REQUEST_ID_HEADER = "X-Request-ID"

fun Application.handleRequest() {
    val idGenerator: ApplicationIdUtil.Generator by inject()
    val credentialsRepo: CredentialsRepo by inject()
    val userRepo: UserRepo by inject()

    install(DefaultHeaders)
    install(CallLogging)
    install(CallId) {
        generate { idGenerator.requestId() }
        verify { it.isNotEmpty() }
        replyToHeader(headerName = REQUEST_ID_HEADER)
    }
    install(GlobalCallData)
    install(MicrometerMetrics) {
        registry = MicrometerConfigs.getRegistry()
        meterBinders = MicrometerConfigs.getBinders()
    }
    install(ContentNegotiation) {
        // TODO: Switch to kotlinx.serialization
        gson()
    }
    install(StatusPages) {
        // TODO: Logic to update error message

        /* Exceptions to handle:
         * CustomExceptions:
         * - EntityAlreadyExistsException - 400
         * - EntityNotFoundException      - 404
         * - InternalException            - 500
         *
         * - DeleteOrUpdateWithoutWhereException - 500
         *
         * UsedExceptions:
         * - IllegalArgumentException
         * - DataAccessException
         */
    }
    install(AutoHeadResponse) // see http://ktor.io/features/autoheadresponse.html
    install(HSTS, applicationHstsConfiguration()) // see http://ktor.io/features/hsts.html
    install(Compression, applicationCompressionConfiguration()) // see http://ktor.io/features/compression.html
    install(Locations) // see http://ktor.io/features/locations.html
    install(Audit)
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
                return@validate tokenCredential.value?.let {
                    return@let credentialsRepo.fetchByRefreshToken(it)
                        ?.let { credential -> userRepo.fetchByHrn(credential.userHrn) }
                        ?.let { user -> UserPrincipal(tokenCredential, user.hrn) }
                }
            }
        }
    }

    // Create a signing Master key pair in case one doesn't exist
    val masterKeysRepo: MasterKeysRepo by inject()
    masterKeysRepo.rotateKey(skipIfPresent = true)

    install(Authorization) {
        isDevelopment = true // TODO: Update the value based on the environment variable.
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

fun Application.module() {
    install(Koin) {
        SLF4JLogger()
        modules(repositoryModule, controllerModule, applicationModule)
    }
    handleRequest()
}

@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    // https://www.baeldung.com/java-bouncy-castle#setup-unlimited-strength-jurisdiction-policy-files
    Security.setProperty("crypto.policy", "unlimited")

    embeddedServer(Netty, 8081, module = Application::module).start(wait = true)
}
