@file:Suppress("LongMethod", "ComplexMethod")

package com.hypto.iam.server

import com.hypto.iam.server.Constants.Companion.SECRET_PREFIX
import com.hypto.iam.server.apis.actionApi
import com.hypto.iam.server.apis.createOrganizationApi
import com.hypto.iam.server.apis.credentialApi
import com.hypto.iam.server.apis.deleteOrganizationApi
import com.hypto.iam.server.apis.getAndUpdateOrganizationApi
import com.hypto.iam.server.apis.keyApi
import com.hypto.iam.server.apis.passcodeApi
import com.hypto.iam.server.apis.policyApi
import com.hypto.iam.server.apis.resetPasswordApi
import com.hypto.iam.server.apis.resourceApi
import com.hypto.iam.server.apis.tokenApi
import com.hypto.iam.server.apis.usersApi
import com.hypto.iam.server.apis.validationApi
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.models.ResetPasswordRequest
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.Authorization
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UsernamePasswordCredential
import com.hypto.iam.server.security.apiKeyAuth
import com.hypto.iam.server.security.bearer
import com.hypto.iam.server.service.DatabaseFactory.pool
import com.hypto.iam.server.service.UserPrincipalService
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.validators.validate
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.db.DatabaseHealthCheck
import com.sksamuel.cohort.hikari.HikariDataSourceManager
import com.sksamuel.cohort.hikari.HikariPendingThreadsHealthCheck
import com.sksamuel.cohort.ktor.Cohort
import com.sksamuel.cohort.ktor.EngineShutdownHook
import com.sksamuel.cohort.logback.LogbackManager
import com.sksamuel.cohort.threads.ThreadDeadlockHealthCheck
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.cio.CIO
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Routing
import io.ktor.util.AttributeKey
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import java.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger

private const val REQUEST_ID_HEADER = "X-Request-Id"
private val shutdownHook = EngineShutdownHook(1.seconds, 1.minutes, 5.minutes)
private const val MAX_THREADS_WAITING_FOR_DB_CONNS = 100
// we need a typed key for using call attributes
// particular name and value in CallStartTime makes no big difference, just for better debugging and readability
@OptIn(ExperimentalTime::class)
val CALL_START_TIME = AttributeKey<TimeMark>("CallStartTime")

@OptIn(ExperimentalTime::class)
fun Application.handleRequest() {
    val idGenerator: ApplicationIdUtil.Generator by inject()
    val appConfig: AppConfig by inject()
    val passcodeRepo: PasscodeRepo by inject()
    val userPrincipalService: UserPrincipalService by inject()
    val micrometerRegistry: MeterRegistry by inject()
    val micrometerBindings: List<MeterBinder> by inject()

    intercept(ApplicationCallPipeline.Setup) {
        // intercept before calling routing and mark every incoming call with a TimeMark
        call.attributes.put(CALL_START_TIME, TimeSource.Monotonic.markNow())
    }
    install(DefaultHeaders)
    install(CallLogging) {
        callIdMdc("call-id")
        format { call ->
            val time = when (val startTime = call.attributes.getOrNull(CALL_START_TIME)) {
                null -> "" // just in case
                else -> startTime.elapsedNow().toString()
            }
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, time: $time"
        }
    }
    install(CallId) {
        retrieveFromHeader(REQUEST_ID_HEADER)
        generate { idGenerator.requestId() }
        verify { it.isNotEmpty() }
        replyToHeader(headerName = REQUEST_ID_HEADER)
    }
    install(MicrometerMetrics) {
        registry = micrometerRegistry
        meterBinders = micrometerBindings
        @Suppress("MagicNumber")
        timers { _, _ ->
            publishPercentileHistogram(true)
            publishPercentiles(0.9, 0.95)
            maximumExpectedValue(Duration.ofSeconds(20)) // Upper bound to limit buckets in histograms
        }
    }
    install(ContentNegotiation) {
        // TODO: Switch to kotlinx.serialization
        gson()
    }
    install(StatusPages) {
        statusPages()
    }
    install(DoubleReceive)
    install(AutoHeadResponse) // see http://ktor.io/features/autoheadresponse.html
    install(HSTS, applicationHstsConfiguration()) // see http://ktor.io/features/hsts.html
    install(Compression, applicationCompressionConfiguration()) // see http://ktor.io/features/compression.html
    install(Authentication) {
        apiKeyAuth("hypto-iam-root-auth") {
            val secretKey = SECRET_PREFIX + appConfig.app.secretKey
            validate { tokenCredential: TokenCredential ->
                when (tokenCredential.value) {
                    secretKey -> ApiPrincipal(tokenCredential, "hypto-root")
                    else -> null
                }
            }
        }
        apiKeyAuth("signup-passcode-auth") {
            validate { tokenCredential: TokenCredential ->
                tokenCredential.value?.let {
                    passcodeRepo.getValidPasscode(it, VerifyEmailRequest.Purpose.signup)?.let {
                        ApiPrincipal(tokenCredential, "hypto-root")
                    }
                }
            }
        }
        apiKeyAuth("reset-passcode-auth") {
            validate { tokenCredential: TokenCredential ->
                val email = this.receive<ResetPasswordRequest>().validate().email
                tokenCredential.value?.let {
                    passcodeRepo.getValidPasscode(it, VerifyEmailRequest.Purpose.reset, email)?.let {
                        ApiPrincipal(tokenCredential, "hypto-root")
                    }
                }
            }
        }
        basic("basic-auth") {
            validate { credentials ->
                val organizationId = this.parameters["organization_id"]!!
                val principal = userPrincipalService.getUserPrincipalByCredentials(
                    organizationId, credentials.name,
                    credentials.password
                )
                if (principal != null) {
                    response.headers.append(Constants.X_ORGANIZATION_HEADER, organizationId)
                }
                return@validate principal
            }
        }
        bearer("bearer-auth") {
            validate { tokenCredential: TokenCredential ->
                if (tokenCredential.value == null) {
                    return@validate null
                }
                return@validate when (tokenCredential.type) {
                    TokenType.CREDENTIAL -> userPrincipalService.getUserPrincipalByRefreshToken(tokenCredential)
                    TokenType.JWT -> userPrincipalService.getUserPrincipalByJwtToken(tokenCredential)
                    else -> throw InternalException("Invalid token credential")
                }
            }
        }
        basic("unique-basic-auth") {
            validate { credentials ->
                if (!appConfig.app.uniqueUsersAcrossOrganizations)
                    throw BadRequestException(
                        "Email not unique across organizations. " +
                            "Please use Token APIs with organization ID"
                    )

                val principal = userPrincipalService.getUserPrincipalByCredentials(
                    UsernamePasswordCredential(credentials.name, credentials.password)
                )
                response.headers.append(Constants.X_ORGANIZATION_HEADER, principal.organization)
                return@validate principal
            }
        }
    }

    // Create a signing Master key pair in case one doesn't exist
    val masterKeysRepo: MasterKeysRepo by inject()
    runBlocking {
        masterKeysRepo.rotateKey(skipIfPresent = true)
    }

    install(Authorization) {
    }

    install(IgnoreTrailingSlash) {}

    install(Routing) {

        authenticate("hypto-iam-root-auth", "signup-passcode-auth") {
            createOrganizationApi()
        }

        authenticate("hypto-iam-root-auth") {
            deleteOrganizationApi()
        }

        authenticate("bearer-auth") {
            getAndUpdateOrganizationApi()
            actionApi()
            credentialApi()
            policyApi()
            resourceApi()
            usersApi()
            validationApi()
        }

        authenticate("reset-passcode-auth") {
            resetPasswordApi()
        }

        tokenApi() // Authentication handled along with API definitions
        keyApi()
        passcodeApi()
    }

    install(Cohort) {
        endpointPrefix = "admin"
        logManager = LogbackManager
        // show connection pool information
        dataSources = listOf(HikariDataSourceManager(pool))

        healthcheck(
            "/status",
            HealthCheckRegistry(Dispatchers.Default) {
                // detects if threads are mutually blocked on each others locks
                register("ThreadDeadlockHealthCheck", ThreadDeadlockHealthCheck(), ZERO, 1.minutes)
                register(
                    "HikariPendingThreadsHealthCheck",
                    HikariPendingThreadsHealthCheck(pool, MAX_THREADS_WAITING_FOR_DB_CONNS),
                    ZERO,
                    1.minutes
                )
                register("DatabaseHealthCheck", DatabaseHealthCheck(pool), ZERO, 1.minutes)
                // TODO: Configure the below check based on infra set-up
                // register(DiskSpaceHealthCheck(/*FileStore for root(/) */, 1.0)), 1.minutes)
            }
        )
        onShutdown(shutdownHook)
    }
}

fun Application.module() {
    install(Koin) {
        SLF4JLogger()
        modules(repositoryModule, controllerModule, applicationModule)
    }
    handleRequest()
}

fun main() {
    val appConfig: AppConfig = AppConfig.configuration

    embeddedServer(
        CIO, appConfig.server.port, module = Application::module,
        configure = {
            // Refer io.ktor.server.engine.ApplicationEngine.Configuration
            connectionGroupSize = appConfig.server.connectionGroupSize
            workerGroupSize = appConfig.server.workerGroupSize
            callGroupSize = appConfig.server.callGroupSize
        }
    ).apply {
        shutdownHook.setEngine(this)
        addShutdownHook { runBlocking { shutdownHook.run() } }
    }.start(wait = true)
}
