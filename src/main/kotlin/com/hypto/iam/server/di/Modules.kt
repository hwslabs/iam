package com.hypto.iam.server.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.hypto.iam.server.MicrometerConfigs
import com.hypto.iam.server.authProviders.AuthProviderRegistry
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.AuthProviderRepo
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.PolicyTemplatesRepo
import com.hypto.iam.server.db.repositories.PrincipalPoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.repositories.UserAuthProvidersRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.idp.CognitoIdentityProviderImpl
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.service.ActionService
import com.hypto.iam.server.service.ActionServiceImpl
import com.hypto.iam.server.service.AuthProviderService
import com.hypto.iam.server.service.AuthProviderServiceImpl
import com.hypto.iam.server.service.CredentialService
import com.hypto.iam.server.service.CredentialServiceImpl
import com.hypto.iam.server.service.MasterKeyCache
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.service.OrganizationsServiceImpl
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.service.PasscodeServiceImpl
import com.hypto.iam.server.service.PolicyService
import com.hypto.iam.server.service.PolicyServiceImpl
import com.hypto.iam.server.service.PolicyTemplatesService
import com.hypto.iam.server.service.PolicyTemplatesServiceImpl
import com.hypto.iam.server.service.PrincipalPolicyService
import com.hypto.iam.server.service.PrincipalPolicyServiceImpl
import com.hypto.iam.server.service.ResourceService
import com.hypto.iam.server.service.ResourceServiceImpl
import com.hypto.iam.server.service.TokenService
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.service.UserAuthService
import com.hypto.iam.server.service.UserAuthServiceImpl
import com.hypto.iam.server.service.UserPrincipalService
import com.hypto.iam.server.service.UserPrincipalServiceImpl
import com.hypto.iam.server.service.UsersService
import com.hypto.iam.server.service.UsersServiceImpl
import com.hypto.iam.server.service.ValidationService
import com.hypto.iam.server.service.ValidationServiceImpl
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.EncryptUtil
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IdGenerator
import com.hypto.iam.server.utils.policy.PolicyValidator
import com.txman.TxMan
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.ses.SesClient

private val log = KotlinLogging.logger { }
const val MAX_IDLE_CONNECTIONS = 50
const val KEEP_ALIVE_DURATION = 5L

// DI module to get repositories
val repositoryModule = module {
    single { MasterKeysRepo }
    single { CredentialsRepo }
    single { OrganizationRepo }
    single { PoliciesRepo }
    single { ResourceRepo }
    single { ActionRepo }
    single { UserAuthProvidersRepo }
    single { PrincipalPoliciesRepo }
    single { PasscodeRepo }
    single { UserRepo }
    single { PolicyTemplatesRepo }
    single { AuthProviderRepo }
    single { UserAuthRepo }
}

val controllerModule = module {
    single { OrganizationsServiceImpl() } bind OrganizationsService::class
    single { TokenServiceImpl() } bind TokenService::class
    single { CredentialServiceImpl() } bind CredentialService::class
    single { PolicyServiceImpl() } bind PolicyService::class
    single { ValidationServiceImpl() } bind ValidationService::class
    single { PrincipalPolicyServiceImpl() } bind PrincipalPolicyService::class
    single { ResourceServiceImpl() } bind ResourceService::class
    single { ActionServiceImpl() } bind ActionService::class
    single { UsersServiceImpl() } bind UsersService::class
    single { UserPrincipalServiceImpl() } bind UserPrincipalService::class
    single { PasscodeServiceImpl() } bind PasscodeService::class
    single { PolicyTemplatesServiceImpl() } bind PolicyTemplatesService::class
    single { AuthProviderServiceImpl() } bind AuthProviderService::class
    single { UserAuthServiceImpl() } bind UserAuthService::class
}

val applicationModule = module {
    single { getGsonInstance() }
    single { MicrometerConfigs.getRegistry() }
    single { MicrometerConfigs.getBinders() }
    single { IdGenerator }
    single { HrnFactory }
    single { EncryptUtil }
    single { ApplicationIdUtil.Generator }
    single { ApplicationIdUtil.Validator }
    single { PolicyValidator }
    single { AppConfig.configuration }
    single { MasterKeyCache }
    single { CognitoIdentityProviderImpl() } bind IdentityProvider::class
    single {
        getCredentialsProvider(
            get<AppConfig>().aws.accessKey,
            get<AppConfig>().aws.secretKey
        )
    } bind AwsCredentialsProvider::class
    single { getCognitoIdentityProviderClient(get<AppConfig>().aws.region, get()) }
    single { getSesClient(get<AppConfig>().aws.region, get()) }
    single { TxMan(com.hypto.iam.server.service.DatabaseFactory.getConfiguration()) }
    single {
        OkHttpClient().newBuilder().apply {
            if (get<AppConfig>().app.isDevelopment || log.isDebugEnabled) {
                this.addInterceptor(
                    HttpLoggingInterceptor {
                        if (log.isDebugEnabled) {
                            log.debug { it }
                        } else {
                            log.info { it }
                        }
                    }.setLevel(HttpLoggingInterceptor.Level.BODY)
                )
            }
        }.connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.MINUTES))
    }
    single(named("AuthProvider")) { get<OkHttpClient.Builder>().build() }
    single { AuthProviderRegistry }
}

fun getCognitoIdentityProviderClient(
    region: String,
    credentialsProvider: AwsCredentialsProvider
): CognitoIdentityProviderClient =
    CognitoIdentityProviderClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider).build()

fun getSesClient(
    region: String,
    credentialsProvider: AwsCredentialsProvider
): SesClient = SesClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider).build()

fun getCredentialsProvider(accessKey: String, secretKey: String): StaticCredentialsProvider =
    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

private fun getGsonInstance(): Gson {
    val isoDateTimeFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_DATE_TIME)
        .parseDefaulting(ChronoField.OFFSET_SECONDS, 0) // UTC if no timezone offset specified
        .toFormatter()

    val dateFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .toFormatter()

    return GsonBuilder()
        .registerTypeAdapter(
            OffsetDateTime::class.java,
            JsonDeserializer { json, _, _ -> OffsetDateTime.from(isoDateTimeFormatter.parse(json.asString)) }
        )
        .registerTypeAdapter(
            OffsetDateTime::class.java,
            JsonSerializer<OffsetDateTime> { date, _, _ -> JsonPrimitive(isoDateTimeFormatter.format(date)) }
        )
        .registerTypeAdapter(
            LocalDate::class.java,
            JsonDeserializer { json, _, _ -> LocalDate.from(dateFormatter.parse(json.asString)) }
        )
        .registerTypeAdapter(
            LocalDate::class.java,
            JsonSerializer<LocalDate> { date, _, _ -> JsonPrimitive(dateFormatter.format(date)) }
        )
        .create()
}

/**
 * Used to inject a KoinComponent into a class / object as an extension.
 *
 * E.g: to inject gson into Policy model from some util class, do
 * val Policy.Companion.gson: Gson
 *    get() = getKoinInstance()
 *
 * This gson attribute can now be used in any other extension function of the Policy class
 */
inline fun <reified T> getKoinInstance(): T {
    return object : KoinComponent {
        val value: T by inject()
    }.value
}
