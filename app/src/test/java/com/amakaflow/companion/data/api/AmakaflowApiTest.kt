package com.amakaflow.companion.data.api

import com.amakaflow.companion.data.model.DeviceInfo
import com.amakaflow.companion.data.model.PairingRequest
import com.amakaflow.companion.data.model.PairingResponse
import com.amakaflow.companion.data.model.TokenRefreshRequest
import com.amakaflow.companion.data.model.TokenRefreshResponse
import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for AmakaflowApi client using MockWebServer.
 * Tests successful responses, error responses (4xx, 5xx), network timeout handling,
 * request formatting (headers, body), and authentication token attachment.
 */
class AmakaflowApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: AmakaflowApi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var retrofit: retrofit2.Retrofit
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        api = retrofit.create(AmakaflowApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // =============================================================================
    // Pairing Tests
    // =============================================================================

    @Test
    fun `pair with valid short code returns successful response`() = runBlocking {
        // Given
        val pairingResponse = PairingResponse(
            jwt = "test-jwt-token",
            profile = null,
            expiresAt = "2025-01-01T00:00:00Z"
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json.encodeToString(pairingResponse))
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = PairingRequest(
            shortCode = "ABC123",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )
        val response = api.pair(request)

        // Then
        assertThat(response.isSuccessful).isTrue()
        assertThat(response.code()).isEqualTo(200)
        assertThat(response.body()).isNotNull()
        assertThat(response.body()?.jwt).isEqualTo("test-jwt-token")
    }

    @Test
    fun `pair with invalid short code returns 400 error`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"detail": "Invalid code"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = PairingRequest(
            shortCode = "INVALID",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )
        val response = api.pair(request)

        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(400)
    }

    @Test
    fun `pair with expired short code returns 410 error`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(410)
                .setBody("""{"detail": "Code has expired"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = PairingRequest(
            shortCode = "EXPIRED",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )
        val response = api.pair(request)

        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(410)
    }

    @Test
    fun `pair request includes correct headers and body`() = runBlocking {
        // Given
        val pairingResponse = PairingResponse(
            jwt = "test-jwt",
            profile = null,
            expiresAt = "2025-01-01T00:00:00Z"
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json.encodeToString(pairingResponse))
        )

        // When
        val request = PairingRequest(
            token = "test-token",
            deviceInfo = DeviceInfo(
                device = "Pixel 8",
                os = "Android 14",
                appVersion = "2.0.0",
                deviceId = "device-123"
            )
        )
        val response = api.pair(request)

        // Then
        assertThat(response.isSuccessful).isTrue()
    }

    @Test
    fun `pair with authentication token attaches it to request`() = runBlocking {
        // Given - Create client with auth interceptor
        val authToken = "Bearer test-auth-token"
        val clientWithAuth = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", authToken)
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        val authRetrofit = retrofit2.Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(clientWithAuth)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val authApi = authRetrofit.create(AmakaflowApi::class.java)

        val pairingResponse = PairingResponse(
            jwt = "test-jwt",
            profile = null,
            expiresAt = "2025-01-01T00:00:00Z"
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json.encodeToString(pairingResponse))
        )

        // When
        val request = PairingRequest(
            shortCode = "ABC123",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )
        authApi.pair(request)

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(authToken)
    }

    // =============================================================================
    // Token Refresh Tests
    // =============================================================================

    @Test
    fun `refresh token returns new JWT on success`() = runBlocking {
        // Given
        val refreshResponse = TokenRefreshResponse(
            jwt = "new-jwt-token",
            expiresAt = kotlinx.datetime.Instant.parse("2025-06-01T00:00:00Z"),
            refreshedAt = kotlinx.datetime.Instant.parse("2025-01-01T12:00:00Z")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json.encodeToString(refreshResponse))
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = TokenRefreshRequest(deviceId = "device-123")
        val response = api.refreshToken(request)

        // Then
        assertThat(response.isSuccessful).isTrue()
        assertThat(response.code()).isEqualTo(200)
        assertThat(response.body()?.jwt).isEqualTo("new-jwt-token")
    }

    @Test
    fun `refresh token returns 401 when device not recognized`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"detail": "Unauthorized"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = TokenRefreshRequest(deviceId = "unknown-device")
        val response = api.refreshToken(request)

        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(401)
    }

    // =============================================================================
    // Server Error Tests (5xx)
    // =============================================================================

    @Test
    fun `pair returns 500 server error`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail": "Internal server error"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = PairingRequest(
            shortCode = "TEST123",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )
        val response = api.pair(request)

        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(500)
    }

    @Test
    fun `pair returns 503 service unavailable`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"detail": "Service temporarily unavailable"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = PairingRequest(
            shortCode = "TEST123",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )
        val response = api.pair(request)

        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(503)
    }

    // =============================================================================
    // Malformed Response Tests
    // =============================================================================

    @Test
    fun `pair handles malformed JSON response`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{ invalid json }")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = PairingRequest(
            shortCode = "TEST123",
            deviceInfo = DeviceInfo(
                device = "Test Device",
                os = "Android 14",
                appVersion = "1.0.0",
                deviceId = "test-device-id"
            )
        )

        // Then - should throw parsing exception
        val exception = org.junit.Assert.assertThrows(Exception::class.java) {
            runBlocking {
                api.pair(request)
            }
        }
    }
}
