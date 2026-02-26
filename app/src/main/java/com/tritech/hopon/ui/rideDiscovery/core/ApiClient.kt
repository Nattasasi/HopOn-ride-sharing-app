package com.tritech.hopon.ui.rideDiscovery.core

import android.content.Context
import com.tritech.hopon.BuildConfig
import com.tritech.hopon.utils.SessionManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit client for the car_pool REST API.
 *
 * - Base URL is resolved at runtime by [ApiBaseUrlResolver] from
 *   `BuildConfig.API_BASE_URL_EMULATOR` and `BuildConfig.API_BASE_URL_DEVICE`
 *   (set via `local.properties`).
 * - JWT token is read from [SessionManager] on every request so it always reflects the
 *   current session state without needing to rebuild the client.
 * - A token-refresh [Authenticator] automatically calls `/auth/refresh` on HTTP 401 and
 *   retries the original request with the new token.
 * - HTTP body logging is enabled only on debug builds.
 */
object ApiClient {

    @Volatile
    private var retrofit: Retrofit? = null

    /**
     * Returns the shared [Retrofit] instance, creating it on first call with the provided
     * application [context] for [SessionManager] token lookups.
     */
    fun getInstance(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context.applicationContext).also { retrofit = it }
        }
    }

    private fun buildRetrofit(appContext: Context): Retrofit {
        val apiBaseUrl = ApiBaseUrlResolver.resolve()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val tokenRefreshAuthenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                // Avoid infinite refresh loops — give up after first retry.
                if (response.request.header("X-Token-Refreshed") != null) return null

                val refreshToken = SessionManager.getRefreshToken(appContext) ?: return null

                // Synchronous refresh call (must not use the main OkHttpClient to avoid deadlock).
                val refreshClient = OkHttpClient()
                val refreshRetrofit = Retrofit.Builder()
                    .baseUrl(apiBaseUrl)
                    .client(refreshClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return try {
                    val authService = refreshRetrofit.create(AuthService::class.java)
                    // Execute synchronously inside the Authenticator callback.
                    val call = refreshRetrofit
                        .create(AuthService::class.java)
                        .let {
                            // Use blocking Retrofit call via raw OkHttp to avoid coroutine context issues.
                            val mediaType = "application/json".toMediaType()
                            val refreshRequest = okhttp3.Request.Builder()
                                .url("${apiBaseUrl}auth/refresh")
                                .post(
                                    "{\"refreshToken\":\"$refreshToken\"}"
                                        .toRequestBody(mediaType)
                                )
                                .build()
                            refreshClient.newCall(refreshRequest).execute()
                        }

                    if (!call.isSuccessful) return null

                    val body = call.body?.string() ?: return null
                    val newToken = org.json.JSONObject(body).optString("token")
                        .takeIf { it.isNotBlank() } ?: return null

                    SessionManager.setToken(appContext, newToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .header("X-Token-Refreshed", "true")
                        .build()
                } catch (_: Exception) {
                    null
                }
            }
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Attach JWT Authorization header if a token is stored in the session.
                val token = SessionManager.getToken(appContext)
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenRefreshAuthenticator)
            .build()

        return Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Creates a typed service interface backed by this client.
     * Example: `ApiClient.create<AuthService>(context)`
     */
    inline fun <reified T> create(context: Context): T =
        getInstance(context).create(T::class.java)
}
