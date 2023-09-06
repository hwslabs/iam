package com.hypto.iam.server.authProviders

import com.google.gson.Gson
import com.hypto.iam.server.ROOT_ORG
import com.hypto.iam.server.exceptions.UnknownException
import com.hypto.iam.server.logger
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential
import io.ktor.http.HttpStatusCode
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val PROFILE_URL = "https://www.googleapis.com/oauth2/v3/userinfo"
const val ACCESS_TOKEN_KEY = "access_token"
object GoogleAuthProvider : BaseAuthProvider, KoinComponent {
    val gson: Gson by inject()
    private val httpClient: OkHttpClient by inject()

    override fun getProfileDetails(tokenCredential: TokenCredential): OAuthUserPrincipal {
        val requestBuilder = Request.Builder()
            .url("$PROFILE_URL?$ACCESS_TOKEN_KEY=${tokenCredential.value}")
            .method("GET", null)
            .addHeader("Content-Type", "application/json")
        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            logger.error("Status code: ${response.code} Body: ${response.body?.string()}")
            if (response.code == HttpStatusCode.Unauthorized.value) {
                throw AuthenticationException("Invalid access token")
            }
            throw UnknownException("Unable to fetch user details")
        }
        val responseBody = response.body?.let { String(it.bytes()) }
        val googleUser = this.gson.fromJson(responseBody, GoogleUser::class.java)
        return OAuthUserPrincipal(
            tokenCredential,
            ROOT_ORG,
            googleUser.email,
            googleUser.name,
            googleUser.hd ?: ""
        )
    }
}

data class GoogleUser(
    val email: String,
    val name: String,
    val hd: String? = null
)
