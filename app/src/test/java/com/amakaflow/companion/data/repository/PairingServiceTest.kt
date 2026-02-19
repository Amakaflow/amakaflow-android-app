package com.amakaflow.companion.data.repository

import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.local.SecureStorage
import com.amakaflow.companion.data.model.PairingResponse
import com.amakaflow.companion.data.model.TokenRefreshResponse
import com.amakaflow.companion.domain.Result
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for PairingService (PairingRepositoryImpl) testing the QR code pairing flow.
 * These tests verify the repository behavior with mocked dependencies.
 */
class PairingServiceTest {

    private lateinit var pairingService: PairingRepositoryImpl
    private lateinit var mockApi: AmakaflowApi
    private lateinit var mockSecureStorage: SecureStorage
    private lateinit var json: Json

    @Before
    fun setup() {
        mockApi = mockk()
        mockSecureStorage = mockk(relaxed = true)
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
        }

        // Default behavior: no token stored
        every { mockSecureStorage.getToken() } returns null
        every { mockSecureStorage.getUserProfile() } returns null

        pairingService = PairingRepositoryImpl(mockApi, mockSecureStorage, json)
    }

    // =============================================================================
    // Token Refresh Tests
    // =============================================================================

    @Test
    fun `refreshToken succeeds and saves new token`() = runBlocking {
        // Given
        every { mockSecureStorage.getDeviceId() } returns "device-123"
        
        coEvery { mockApi.refreshToken(any()) } returns Response.success(
            TokenRefreshResponse(
                jwt = "new-jwt-token",
                expiresAt = kotlinx.datetime.Instant.parse("2025-06-01T00:00:00Z"),
                refreshedAt = kotlinx.datetime.Instant.parse("2025-01-01T12:00:00Z")
            )
        )

        // When
        val result = pairingService.refreshToken()

        // Then
        assertThat(result).isTrue()
        coVerify { mockSecureStorage.saveToken("new-jwt-token") }
    }

    @Test
    fun `refreshToken fails with 401 and marks needsReauth`() = runBlocking {
        // Given
        every { mockSecureStorage.getDeviceId() } returns "device-123"
        
        coEvery { mockApi.refreshToken(any()) } returns Response.error(
            401,
            """{"detail": "Unauthorized"}""".toResponseBody()
        )

        // When
        val result = pairingService.refreshToken()

        // Then
        assertThat(result).isFalse()
        assertThat(pairingService.needsReauth.value).isTrue()
    }

    @Test
    fun `refreshToken handles network exception`() = runBlocking {
        // Given
        every { mockSecureStorage.getDeviceId() } returns "device-123"
        
        coEvery { mockApi.refreshToken(any()) } throws java.net.SocketException("Network error")

        // When
        val result = pairingService.refreshToken()

        // Then
        assertThat(result).isFalse()
    }

    // =============================================================================
    // State Tests
    // =============================================================================

    @Test
    fun `isPaired reflects token storage state on init with token`() {
        // Given - token exists
        every { mockSecureStorage.getToken() } returns "existing-token"
        
        // When - create new instance
        val serviceWithToken = PairingRepositoryImpl(mockApi, mockSecureStorage, json)

        // Then
        assertThat(serviceWithToken.isPaired.value).isTrue()
    }

    @Test
    fun `isPaired is false when no token on init`() {
        // Given - no token
        every { mockSecureStorage.getToken() } returns null
        
        // When - create new instance
        val serviceWithoutToken = PairingRepositoryImpl(mockApi, mockSecureStorage, json)

        // Then
        assertThat(serviceWithoutToken.isPaired.value).isFalse()
    }

    @Test
    fun `markAuthInvalid sets needsReauth to true`() {
        // When
        pairingService.markAuthInvalid()

        // Then
        assertThat(pairingService.needsReauth.value).isTrue()
    }

    // =============================================================================
    // Unpair Tests
    // =============================================================================

    @Test
    fun `unpair clears token and profile`() = runBlocking {
        // Given - First pair to set state
        every { mockSecureStorage.getToken() } returns "existing-token"
        
        // When
        pairingService.unpair()

        // Then
        coVerify { mockSecureStorage.clearToken() }
        coVerify { mockSecureStorage.clearUserProfile() }
        assertThat(pairingService.isPaired.value).isFalse()
        assertThat(pairingService.userProfile.value).isNull()
    }
}
