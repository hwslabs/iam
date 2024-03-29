package com.hypto.iam.server.authProviders

import com.google.gson.Gson
import com.hypto.iam.server.ROOT_ORG
import com.hypto.iam.server.db.tables.records.UserAuthRecord
import com.hypto.iam.server.exceptions.UnknownException
import com.hypto.iam.server.logger
import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential
import io.ktor.http.HttpStatusCode
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object MicrosoftAuthProvider : BaseAuthProvider("microsoft", false), KoinComponent {
    private const val PROFILE_URL = "https://graph.microsoft.com/v1.0/me"

    val gson: Gson by inject()
    private val httpClient: OkHttpClient by inject(named("AuthProvider"))

    override fun getProfileDetails(tokenCredential: TokenCredential): OAuthUserPrincipal {
        val requestBuilder =
            Request.Builder()
                .url(PROFILE_URL)
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${tokenCredential.value}")
        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            logger.error("Status code: ${response.code} Body: ${response.body?.string()}")
            if (response.code == HttpStatusCode.Unauthorized.value) {
                throw AuthenticationException("Invalid access token")
            }
            throw UnknownException("Unable to fetch user details")
        }
        val microsoftUser = response.body?.string()?.let { this.gson.fromJson(it, MicrosoftUser::class.java) }!!
        if (microsoftUser.mail == null) {
            throw AuthenticationException("Email not associated with Microsoft profile")
        }
        return OAuthUserPrincipal(
            tokenCredential,
            ROOT_ORG,
            microsoftUser.mail,
            microsoftUser.displayName,
            "",
            this.providerName,
            AuthMetadata(
                id = microsoftUser.id,
            ),
        )
    }

    override suspend fun authenticate(
        principal: OAuthUserPrincipal,
        userAuthRecord: UserAuthRecord,
    ) {
        val principalMetadata = principal.metadata
        val authMetadata = AuthMetadata.from(userAuthRecord.authMetadata)

        // Reason to use id instead of email is because email can be changed in Microsoft profile
        // Blog: https://0x8.in/blog/2021/04/30/mip-oid-sub/
        if (principalMetadata?.id == null || authMetadata.id == null || authMetadata.id != principalMetadata.id) {
            throw AuthenticationException("User is not authenticated with Microsoft")
        }

        return
    }
}

data class MicrosoftUser(
    val id: String,
    val mail: String? = null,
    val displayName: String,
)
