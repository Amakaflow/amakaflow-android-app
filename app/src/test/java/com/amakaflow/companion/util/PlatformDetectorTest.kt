package com.amakaflow.companion.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlatformDetectorTest {

    // ---- extractUrl ----

    @Test
    fun `extractUrl - clean YouTube URL`() {
        val text = "https://www.youtube.com/watch?v=abc123"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo(text)
    }

    @Test
    fun `extractUrl - YouTube short URL`() {
        val text = "https://youtu.be/abc123"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo(text)
    }

    @Test
    fun `extractUrl - Instagram with caption text`() {
        val text = "Check out this workout! Super intense upper body session https://www.instagram.com/reel/ABC123/"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo("https://www.instagram.com/reel/ABC123/")
    }

    @Test
    fun `extractUrl - TikTok short URL`() {
        val text = "https://vm.tiktok.com/ZMhABC123/"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo(text)
    }

    @Test
    fun `extractUrl - TikTok full URL with caption`() {
        val text = "HIIT workout for beginners https://www.tiktok.com/@user/video/1234567890"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo("https://www.tiktok.com/@user/video/1234567890")
    }

    @Test
    fun `extractUrl - Pinterest URL`() {
        val text = "https://www.pinterest.com/pin/123456789/"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo(text)
    }

    @Test
    fun `extractUrl - Pinterest short URL`() {
        val text = "Cool workout pin https://pin.it/abc123"
        assertThat(PlatformDetector.extractUrl(text)).isEqualTo("https://pin.it/abc123")
    }

    @Test
    fun `extractUrl - no URL returns null`() {
        assertThat(PlatformDetector.extractUrl("just some text with no URL")).isNull()
    }

    @Test
    fun `extractUrl - empty string returns null`() {
        assertThat(PlatformDetector.extractUrl("")).isNull()
    }

    @Test
    fun `extractUrl - whitespace only returns null`() {
        assertThat(PlatformDetector.extractUrl("   ")).isNull()
    }

    // ---- extractAllUrls ----

    @Test
    fun `extractAllUrls - multiple URLs`() {
        val text = "Check these out: https://youtube.com/watch?v=1 and https://www.tiktok.com/@u/video/2"
        val urls = PlatformDetector.extractAllUrls(text)
        assertThat(urls).hasSize(2)
        assertThat(urls[0]).isEqualTo("https://youtube.com/watch?v=1")
        assertThat(urls[1]).isEqualTo("https://www.tiktok.com/@u/video/2")
    }

    @Test
    fun `extractAllUrls - no URLs returns empty`() {
        assertThat(PlatformDetector.extractAllUrls("no urls here")).isEmpty()
    }

    // ---- detectPlatform ----

    @Test
    fun `detectPlatform - YouTube full URL`() {
        assertThat(PlatformDetector.detectPlatform("https://www.youtube.com/watch?v=abc"))
            .isEqualTo(PlatformDetector.Platform.YOUTUBE)
    }

    @Test
    fun `detectPlatform - YouTube short URL`() {
        assertThat(PlatformDetector.detectPlatform("https://youtu.be/abc"))
            .isEqualTo(PlatformDetector.Platform.YOUTUBE)
    }

    @Test
    fun `detectPlatform - Instagram`() {
        assertThat(PlatformDetector.detectPlatform("https://www.instagram.com/reel/ABC/"))
            .isEqualTo(PlatformDetector.Platform.INSTAGRAM)
    }

    @Test
    fun `detectPlatform - TikTok full`() {
        assertThat(PlatformDetector.detectPlatform("https://www.tiktok.com/@user/video/123"))
            .isEqualTo(PlatformDetector.Platform.TIKTOK)
    }

    @Test
    fun `detectPlatform - TikTok short`() {
        assertThat(PlatformDetector.detectPlatform("https://vm.tiktok.com/ZMhABC123/"))
            .isEqualTo(PlatformDetector.Platform.TIKTOK)
    }

    @Test
    fun `detectPlatform - Pinterest full`() {
        assertThat(PlatformDetector.detectPlatform("https://www.pinterest.com/pin/123/"))
            .isEqualTo(PlatformDetector.Platform.PINTEREST)
    }

    @Test
    fun `detectPlatform - Pinterest short`() {
        assertThat(PlatformDetector.detectPlatform("https://pin.it/abc123"))
            .isEqualTo(PlatformDetector.Platform.PINTEREST)
    }

    @Test
    fun `detectPlatform - unknown URL`() {
        assertThat(PlatformDetector.detectPlatform("https://example.com/workout"))
            .isEqualTo(PlatformDetector.Platform.UNKNOWN)
    }

    @Test
    fun `detectPlatform - case insensitive`() {
        assertThat(PlatformDetector.detectPlatform("https://WWW.YOUTUBE.COM/watch?v=ABC"))
            .isEqualTo(PlatformDetector.Platform.YOUTUBE)
    }

    // ---- parseSharedText ----

    @Test
    fun `parseSharedText - Instagram with caption`() {
        val result = PlatformDetector.parseSharedText(
            "My 30 min HIIT workout https://www.instagram.com/reel/CxYz123/"
        )
        assertThat(result).isNotNull()
        assertThat(result!!.url).isEqualTo("https://www.instagram.com/reel/CxYz123/")
        assertThat(result.platform).isEqualTo(PlatformDetector.Platform.INSTAGRAM)
    }

    @Test
    fun `parseSharedText - no URL returns null`() {
        assertThat(PlatformDetector.parseSharedText("no url here")).isNull()
    }

    // ---- Platform properties ----

    @Test
    fun `platform displayName values`() {
        assertThat(PlatformDetector.Platform.YOUTUBE.displayName).isEqualTo("YouTube")
        assertThat(PlatformDetector.Platform.INSTAGRAM.displayName).isEqualTo("Instagram")
        assertThat(PlatformDetector.Platform.TIKTOK.displayName).isEqualTo("TikTok")
        assertThat(PlatformDetector.Platform.PINTEREST.displayName).isEqualTo("Pinterest")
        assertThat(PlatformDetector.Platform.UNKNOWN.displayName).isEqualTo("Link")
    }

    @Test
    fun `platform apiEndpoint values`() {
        assertThat(PlatformDetector.Platform.YOUTUBE.apiEndpoint).isEqualTo("ingest/youtube")
        assertThat(PlatformDetector.Platform.INSTAGRAM.apiEndpoint).isEqualTo("ingest/instagram_reel")
        assertThat(PlatformDetector.Platform.TIKTOK.apiEndpoint).isEqualTo("ingest/tiktok")
        assertThat(PlatformDetector.Platform.PINTEREST.apiEndpoint).isEqualTo("ingest/pinterest")
        assertThat(PlatformDetector.Platform.UNKNOWN.apiEndpoint).isEqualTo("ingest/url")
    }
}
