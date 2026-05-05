package com.amakaflow.companion.ui.screens.settings

import app.cash.turbine.test
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.api.dto.TelegramLinkStatusResponse
import com.amakaflow.companion.data.api.dto.TelegramLinkTokenResponse
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

/**
 * AMA-1758 Phase C — viewmodel state machine tests for the Connect Telegram flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectTelegramViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mintBody = TelegramLinkTokenResponse(
        token = "tok-abc",
        deepLink = "https://t.me/amakaflow_userbot?start=tok-abc",
        nativeLink = "tg://resolve?domain=amakaflow_userbot&start=tok-abc",
        expiresInSeconds = 900,
    )

    @Test
    fun `idle by default`() = runTest {
        val api = mockk<AmakaflowApi>(relaxed = true)
        val vm = ConnectTelegramViewModel(api)
        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Idle)
        assertThat(vm.launchTelegram.value).isNull()
    }

    @Test
    fun `startConnect mints and emits AwaitingPair plus a launch signal`() = runTest {
        val api = mockk<AmakaflowApi>()
        coEvery { api.mintTelegramLinkToken() } returns Response.success(mintBody)
        // Status poll always returns "not yet linked" so we stay in AwaitingPair.
        coEvery { api.getTelegramLinkStatus(any()) } returns Response.success(
            TelegramLinkStatusResponse(linked = false)
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ConnectTelegramUiState.AwaitingPair::class.java)
        s as ConnectTelegramUiState.AwaitingPair
        assertThat(s.token).isEqualTo("tok-abc")
        assertThat(s.nativeLink).contains("amakaflow_userbot")
        assertThat(vm.launchTelegram.value).isEqualTo(s.nativeLink)

        // Cancel so the polling job doesn't bleed into the next test's scheduler.
        vm.cancel()
    }

    @Test
    fun `mint failure surfaces Error state`() = runTest {
        val api = mockk<AmakaflowApi>()
        coEvery { api.mintTelegramLinkToken() } returns Response.error(
            503,
            "{\"detail\":\"down\"}".toResponseBody("application/json".toMediaTypeOrNull()),
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ConnectTelegramUiState.Error::class.java)
    }

    @Test
    fun `polling transitions to Connected when link-status reports linked`() = runTest {
        val api = mockk<AmakaflowApi>()
        coEvery { api.mintTelegramLinkToken() } returns Response.success(mintBody)
        coEvery { api.getTelegramLinkStatus("tok-abc") } returns Response.success(
            TelegramLinkStatusResponse(linked = true, telegramId = 7888191549L, usedAt = "2026-05-04T20:23:27Z")
        )

        val vm = ConnectTelegramViewModel(api)
        vm.state.test {
            assertThat(awaitItem()).isEqualTo(ConnectTelegramUiState.Idle)
            vm.startConnect()
            assertThat(awaitItem()).isEqualTo(ConnectTelegramUiState.Minting)
            assertThat(awaitItem()).isInstanceOf(ConnectTelegramUiState.AwaitingPair::class.java)

            // Drive past the first poll interval; the linked=true response should flip us to Connected.
            advanceTimeBy(ConnectTelegramViewModel.POLL_INTERVAL_MS + 50)
            val terminal = awaitItem()
            assertThat(terminal).isInstanceOf(ConnectTelegramUiState.Connected::class.java)
            assertThat((terminal as ConnectTelegramUiState.Connected).telegramId).isEqualTo(7888191549L)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `polling expires after the server-supplied window when shorter than the cap`() = runTest {
        val api = mockk<AmakaflowApi>()
        // Server says 30s — well below the 15-min cap.
        coEvery { api.mintTelegramLinkToken() } returns Response.success(mintBody.copy(expiresInSeconds = 30))
        coEvery { api.getTelegramLinkStatus("tok-abc") } returns Response.success(
            TelegramLinkStatusResponse(linked = false)
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()

        // Advance past the 30s polling window + one extra tick to trigger Expired.
        advanceTimeBy(30_000L + ConnectTelegramViewModel.POLL_INTERVAL_MS)
        runCurrent()

        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Expired)
    }

    @Test
    fun `polling clamps to MAX_POLL_WINDOW_SECONDS when server TTL is longer`() = runTest {
        val api = mockk<AmakaflowApi>()
        // Server claims 1 hour — should be clamped to 15 minutes.
        coEvery { api.mintTelegramLinkToken() } returns Response.success(mintBody.copy(expiresInSeconds = 3_600))
        coEvery { api.getTelegramLinkStatus("tok-abc") } returns Response.success(
            TelegramLinkStatusResponse(linked = false)
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ConnectTelegramUiState.AwaitingPair::class.java)
        assertThat((s as ConnectTelegramUiState.AwaitingPair).secondsRemaining)
            .isEqualTo(ConnectTelegramViewModel.MAX_POLL_WINDOW_SECONDS)

        vm.cancel()
    }

    @Test
    fun `cancel returns to Idle and stops polling`() = runTest {
        val api = mockk<AmakaflowApi>()
        coEvery { api.mintTelegramLinkToken() } returns Response.success(mintBody)
        coEvery { api.getTelegramLinkStatus(any()) } returns Response.success(
            TelegramLinkStatusResponse(linked = false)
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()
        assertThat(vm.state.value).isInstanceOf(ConnectTelegramUiState.AwaitingPair::class.java)

        vm.cancel()
        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Idle)
        assertThat(vm.launchTelegram.value).isNull()

        // Pushing time forward should NOT now flip us into a terminal state.
        advanceTimeBy(ConnectTelegramViewModel.MAX_POLL_WINDOW_SECONDS * 1_000L)
        runCurrent()
        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Idle)
    }

    @Test
    fun `cancel during in-flight mint does not flip back to AwaitingPair`() = runTest {
        // Mint hangs forever — simulates a slow network request the user cancels mid-flight.
        val api = mockk<AmakaflowApi>()
        coEvery { api.mintTelegramLinkToken() } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        coEvery { api.getTelegramLinkStatus(any()) } returns Response.success(
            TelegramLinkStatusResponse(linked = false)
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()
        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Minting)

        // User taps Cancel while the mint is still in flight.
        vm.cancel()
        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Idle)

        // Even after time passes, we should NOT race into AwaitingPair / launch the intent.
        advanceTimeBy(5_000L)
        runCurrent()
        assertThat(vm.state.value).isEqualTo(ConnectTelegramUiState.Idle)
        assertThat(vm.launchTelegram.value).isNull()
    }

    @Test
    fun `onTelegramLaunched clears the one-shot launch signal`() = runTest {
        val api = mockk<AmakaflowApi>()
        coEvery { api.mintTelegramLinkToken() } returns Response.success(mintBody)
        coEvery { api.getTelegramLinkStatus(any()) } returns Response.success(
            TelegramLinkStatusResponse(linked = false)
        )

        val vm = ConnectTelegramViewModel(api)
        vm.startConnect()
        runCurrent()
        assertThat(vm.launchTelegram.value).isNotNull()

        vm.onTelegramLaunched()
        assertThat(vm.launchTelegram.value).isNull()

        vm.cancel()
    }
}
