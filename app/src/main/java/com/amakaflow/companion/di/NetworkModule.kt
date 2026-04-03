package com.amakaflow.companion.di

import android.util.Log
import com.amakaflow.companion.BuildConfig
import com.amakaflow.companion.data.AppEnvironment
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.api.AuthAuthenticator
import com.amakaflow.companion.data.api.AuthStateManager
import com.amakaflow.companion.data.api.IngestorApi
import com.amakaflow.companion.data.local.SecureStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DynamicMapperUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DynamicIngestorUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    /**
     * Dynamic URL interceptor for Mapper API - reads AppEnvironment.current on each request
     * This allows environment switching without app restart
     */
    @Provides
    @Singleton
    @DynamicMapperUrl
    fun provideMapperUrlInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val currentEnv = AppEnvironment.current
            val targetUrl = currentEnv.mapperApiUrl
            Log.d("NetworkModule", "Environment: $currentEnv, Target URL: $targetUrl")
            val newUrl = originalRequest.url.newBuilder()
                .scheme(targetUrl.toHttpUrl().scheme)
                .host(targetUrl.toHttpUrl().host)
                .port(targetUrl.toHttpUrl().port)
                .build()
            Log.d("NetworkModule", "Final URL: $newUrl")
            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()
            chain.proceed(newRequest)
        }
    }

    /**
     * Dynamic URL interceptor for Ingestor API - reads AppEnvironment.current on each request
     */
    @Provides
    @Singleton
    @DynamicIngestorUrl
    fun provideIngestorUrlInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val newUrl = originalRequest.url.newBuilder()
                .scheme(AppEnvironment.current.ingestorApiUrl.toHttpUrl().scheme)
                .host(AppEnvironment.current.ingestorApiUrl.toHttpUrl().host)
                .port(AppEnvironment.current.ingestorApiUrl.toHttpUrl().port)
                .build()
            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()
            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    @Named("mapperBaseUrl")
    fun provideMapperBaseUrl(): String = "https://placeholder.amakaflow.com/"

    @Provides
    @Singleton
    fun provideAuthStateManager(): AuthStateManager = object : AuthStateManager {
        private var callback: (() -> Unit)? = null

        override fun markNeedsReauth() {
            callback?.invoke()
        }

        fun setCallback(onNeedsReauth: () -> Unit) {
            callback = onNeedsReauth
        }
    }

    @Provides
    @Singleton
    fun provideAuthAuthenticator(
        secureStorage: SecureStorage,
        json: Json,
        @Named("mapperBaseUrl") baseUrlProvider: Provider<String>,
        authStateManager: AuthStateManager
    ): AuthAuthenticator {
        return AuthAuthenticator(secureStorage, json, baseUrlProvider, authStateManager)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        secureStorage: SecureStorage,
        testConfig: TestConfig
    ): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()

            // Check for E2E test mode first (takes precedence)
            if (BuildConfig.DEBUG && testConfig.isTestModeEnabled) {
                testConfig.testAuthSecret?.trim()?.let { secret ->
                    request.addHeader("X-Test-Auth", secret)
                    testConfig.testUserId?.trim()?.let { userId ->
                        request.addHeader("X-Test-User-Id", userId)
                    }
                }
            } else {
                // Add JWT token if available
                secureStorage.getToken()?.let { token ->
                    request.addHeader("Authorization", "Bearer $token")
                }
            }

            request.addHeader("Content-Type", "application/json")
            chain.proceed(request.build())
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @Named("mapper")
    fun provideMapperOkHttpClient(
        authInterceptor: Interceptor,
        authAuthenticator: AuthAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        @DynamicMapperUrl dynamicUrlInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("ingestor")
    fun provideIngestorOkHttpClient(
        authInterceptor: Interceptor,
        authAuthenticator: AuthAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        @DynamicIngestorUrl dynamicUrlInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("mcp")
    fun provideMcpOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        // No dynamic URL interceptor needed: the MCP URL is read directly from AppEnvironment.
        // Use a longer read timeout to accommodate streaming SSE responses.
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Dedicated OkHttpClient for Coach Chat SSE streaming.
     * Uses the dynamic mapper URL interceptor and a long read timeout
     * to sustain the SSE connection for the duration of a response.
     */
    @Provides
    @Singleton
    @Named("coachSse")
    fun provideCoachSseOkHttpClient(
        authInterceptor: Interceptor,
        authAuthenticator: AuthAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        @DynamicMapperUrl dynamicUrlInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("mapper")
    fun provideMapperRetrofit(@Named("mapper") okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        // Use placeholder URL - actual URL is set dynamically by DynamicMapperUrl interceptor
        return Retrofit.Builder()
            .baseUrl("https://placeholder.amakaflow.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("ingestor")
    fun provideIngestorRetrofit(@Named("ingestor") okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        // Use placeholder URL - actual URL is set dynamically by DynamicIngestorUrl interceptor
        return Retrofit.Builder()
            .baseUrl("https://placeholder.amakaflow.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAmakaflowApi(@Named("mapper") retrofit: Retrofit): AmakaflowApi {
        return retrofit.create(AmakaflowApi::class.java)
    }

    @Provides
    @Singleton
    fun provideIngestorApi(@Named("ingestor") retrofit: Retrofit): IngestorApi {
        return retrofit.create(IngestorApi::class.java)
    }
}
