package com.amakaflow.companion.data.worker

import com.amakaflow.companion.util.PlatformDetector
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * AMA-1258: Unit tests for ImportWorker helper logic.
 * The actual Worker execution requires Android instrumentation tests (Context, WorkManager),
 * so here we test the companion-object logic and integration with PlatformDetector.
 */
class ImportWorkerTest {

    @Test
    fun `KEY_URL constant is defined`() {
        assertThat(ImportWorker.KEY_URL).isEqualTo("url")
    }

    @Test
    fun `KEY_PLATFORM constant is defined`() {
        assertThat(ImportWorker.KEY_PLATFORM).isEqualTo("platform")
    }

    @Test
    fun `KEY_WORKOUT_NAME constant is defined`() {
        assertThat(ImportWorker.KEY_WORKOUT_NAME).isEqualTo("workout_name")
    }

    @Test
    fun `KEY_WORKOUT_ID constant is defined`() {
        assertThat(ImportWorker.KEY_WORKOUT_ID).isEqualTo("workout_id")
    }

    @Test
    fun `KEY_ERROR constant is defined`() {
        assertThat(ImportWorker.KEY_ERROR).isEqualTo("error")
    }

    @Test
    fun `platform detection routes YouTube to correct endpoint`() {
        val platform = PlatformDetector.detectPlatform("https://www.youtube.com/watch?v=abc")
        assertThat(platform).isEqualTo(PlatformDetector.Platform.YOUTUBE)
    }

    @Test
    fun `platform detection routes Instagram to correct endpoint`() {
        val platform = PlatformDetector.detectPlatform("https://www.instagram.com/reel/ABC/")
        assertThat(platform).isEqualTo(PlatformDetector.Platform.INSTAGRAM)
    }

    @Test
    fun `platform detection routes TikTok short URL to correct endpoint`() {
        val platform = PlatformDetector.detectPlatform("https://vm.tiktok.com/ZMhABC123/")
        assertThat(platform).isEqualTo(PlatformDetector.Platform.TIKTOK)
    }

    @Test
    fun `platform detection falls back to UNKNOWN for unrecognized URLs`() {
        val platform = PlatformDetector.detectPlatform("https://some-fitness-blog.com/workout/123")
        assertThat(platform).isEqualTo(PlatformDetector.Platform.UNKNOWN)
    }
}
