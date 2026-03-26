package com.amakaflow.companion.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepLinkParserTest {

    // ---- parseString: HTTPS deep links ----

    @Test
    fun `parseString - amakaflow_com import with encoded YouTube URL`() {
        val result = DeepLinkParser.parseString(
            "https://amakaflow.com/import?url=https%3A%2F%2Fyoutu.be%2Fabc123"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://youtu.be/abc123")
    }

    @Test
    fun `parseString - app_amakaflow_com import`() {
        val result = DeepLinkParser.parseString(
            "https://app.amakaflow.com/import?url=https%3A%2F%2Fwww.instagram.com%2Freel%2FABC"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://www.instagram.com/reel/ABC")
    }

    @Test
    fun `parseString - www_amakaflow_com import`() {
        val result = DeepLinkParser.parseString(
            "https://www.amakaflow.com/import?url=https%3A%2F%2Ftiktok.com%2Fvideo"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://tiktok.com/video")
    }

    @Test
    fun `parseString - missing url parameter returns MissingUrlParam`() {
        val result = DeepLinkParser.parseString("https://amakaflow.com/import")
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.MissingUrlParam::class.java)
    }

    @Test
    fun `parseString - blank url parameter returns MissingUrlParam`() {
        val result = DeepLinkParser.parseString("https://amakaflow.com/import?url=")
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.MissingUrlParam::class.java)
    }

    @Test
    fun `parseString - wrong path returns NotADeepLink`() {
        val result = DeepLinkParser.parseString("https://amakaflow.com/workouts?url=abc")
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }

    @Test
    fun `parseString - wrong host returns NotADeepLink`() {
        val result = DeepLinkParser.parseString(
            "https://evil.com/import?url=https%3A%2F%2Fyoutu.be%2Fabc"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }

    @Test
    fun `parseString - http scheme returns NotADeepLink`() {
        val result = DeepLinkParser.parseString(
            "http://amakaflow.com/import?url=https%3A%2F%2Fyoutu.be%2Fabc"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }

    @Test
    fun `parseString - null uri returns NotADeepLink`() {
        val result = DeepLinkParser.parseString(null)
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }

    @Test
    fun `parseString - blank string returns NotADeepLink`() {
        val result = DeepLinkParser.parseString("")
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }

    // ---- parseString: Custom scheme deep links ----

    @Test
    fun `parseString - custom scheme import`() {
        val result = DeepLinkParser.parseString(
            "amakaflow://import?url=https%3A%2F%2Fyoutu.be%2Fabc123"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://youtu.be/abc123")
    }

    @Test
    fun `parseString - custom scheme missing url parameter`() {
        val result = DeepLinkParser.parseString("amakaflow://import")
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.MissingUrlParam::class.java)
    }

    @Test
    fun `parseString - custom scheme wrong host returns NotADeepLink`() {
        val result = DeepLinkParser.parseString("amakaflow://settings")
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }

    // ---- parseString: URL decoding ----

    @Test
    fun `parseString - complex encoded URL is properly decoded`() {
        val encodedUrl = "https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ%26list%3DPLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"
        val result = DeepLinkParser.parseString(
            "https://amakaflow.com/import?url=$encodedUrl"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
    }

    @Test
    fun `parseString - TikTok URL with special characters`() {
        val encodedUrl = "https%3A%2F%2Fwww.tiktok.com%2F%40user%2Fvideo%2F1234567890"
        val result = DeepLinkParser.parseString(
            "https://amakaflow.com/import?url=$encodedUrl"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://www.tiktok.com/@user/video/1234567890")
    }

    // ---- isAmakaFlowDeepLink (string overload) ----

    @Test
    fun `isAmakaFlowDeepLink - amakaflow_com returns true`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink("https://amakaflow.com/anything")).isTrue()
    }

    @Test
    fun `isAmakaFlowDeepLink - app_amakaflow_com returns true`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink("https://app.amakaflow.com/anything")).isTrue()
    }

    @Test
    fun `isAmakaFlowDeepLink - www_amakaflow_com returns true`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink("https://www.amakaflow.com/anything")).isTrue()
    }

    @Test
    fun `isAmakaFlowDeepLink - custom scheme returns true`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink("amakaflow://import")).isTrue()
    }

    @Test
    fun `isAmakaFlowDeepLink - other domain returns false`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink("https://google.com/import")).isFalse()
    }

    @Test
    fun `isAmakaFlowDeepLink - null returns false`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink(null as String?)).isFalse()
    }

    @Test
    fun `isAmakaFlowDeepLink - empty returns false`() {
        assertThat(DeepLinkParser.isAmakaFlowDeepLink("")).isFalse()
    }

    // ---- Edge cases ----

    @Test
    fun `parseString - extra query params are ignored`() {
        val result = DeepLinkParser.parseString(
            "https://amakaflow.com/import?source=web&url=https%3A%2F%2Fyoutu.be%2Fabc&ref=share"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.ImportUrl::class.java)
        assertThat((result as DeepLinkParser.DeepLinkResult.ImportUrl).url)
            .isEqualTo("https://youtu.be/abc")
    }

    @Test
    fun `parseString - subdomain not in allowlist returns NotADeepLink`() {
        val result = DeepLinkParser.parseString(
            "https://staging.amakaflow.com/import?url=https%3A%2F%2Fyoutu.be%2Fabc"
        )
        assertThat(result).isInstanceOf(DeepLinkParser.DeepLinkResult.NotADeepLink::class.java)
    }
}
