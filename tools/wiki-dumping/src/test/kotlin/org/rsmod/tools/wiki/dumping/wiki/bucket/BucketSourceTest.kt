package org.rsmod.tools.wiki.dumping.wiki.bucket

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BucketSourceTest {
    private fun src(
        vararg responses: String,
        offline: Boolean = false,
    ): Pair<BucketSource, MutableList<String>> {
        val urls = mutableListOf<String>()
        val iter = responses.iterator()
        val source =
            BucketSource(
                cacheDir = Files.createTempDirectory("bucket-test"),
                offline = offline,
                throttleMillis = 0,
                fetch = { url ->
                    urls += url
                    iter.next()
                },
            )
        return source to urls
    }

    @Test
    fun `builds escaped query and parses rows`() =
        runBlocking<Unit> {
            val (source, urls) = src("""{"bucket":[{"sold_item":"Bread"}]}""")
            val rows =
                source.rows(
                    "storeline",
                    listOf("sold_item"),
                    "page_name" to "Bob's Axes",
                    cacheKey = "bobs",
                )
            assertEquals(1, rows.size)
            assertEquals("Bread", rows[0].requireText("sold_item", "test"))
            val decoded = java.net.URLDecoder.decode(urls.single(), "UTF-8")
            assertEquals(true, decoded.contains("where('page_name','Bob\\'s Axes')"))
        }

    @Test
    fun `escapes backslashes before quotes in query values`() =
        runBlocking<Unit> {
            val (source, urls) = src("""{"bucket":[{"sold_item":"Bread"}]}""")
            source.rows(
                "storeline",
                listOf("sold_item"),
                "page_name" to "C:\\Bob's Axes",
                cacheKey = "bobs-backslash",
            )
            val decoded = java.net.URLDecoder.decode(urls.single(), "UTF-8")
            assertEquals(true, decoded.contains("where('page_name','C:\\\\Bob\\'s Axes')"))
        }

    @Test
    fun `pages with offset until short page`() =
        runBlocking<Unit> {
            val full = (1..3).joinToString(",") { """{"n":"$it"}""" }
            val (source, urls) = src("""{"bucket":[$full]}""", """{"bucket":[{"n":"4"}]}""")
            val rows = source.rows("b", listOf("n"), null, cacheKey = "k", limit = 3)
            assertEquals(4, rows.size)
            assertEquals(2, urls.size)
            assertEquals(true, java.net.URLDecoder.decode(urls[1], "UTF-8").contains(".offset(3)"))
        }

    @Test
    fun `second call hits cache, offline replays cache, offline miss throws`() =
        runBlocking<Unit> {
            val dir = Files.createTempDirectory("bucket-test")
            val one =
                BucketSource(
                    dir,
                    offline = false,
                    throttleMillis = 0,
                    fetch = { """{"bucket":[{"a":"1"}]}""" },
                )
            one.rows("b", listOf("a"), null, cacheKey = "k")
            val offline =
                BucketSource(
                    dir,
                    offline = true,
                    throttleMillis = 0,
                    fetch = { error("no network in offline") },
                )
            assertEquals(1, offline.rows("b", listOf("a"), null, cacheKey = "k").size)
            assertThrows(IllegalStateException::class.java) {
                runBlocking { offline.rows("b", listOf("a"), null, cacheKey = "other") }
            }
        }

    @Test
    fun `api error and missing field throw`() =
        runBlocking<Unit> {
            val (errSource, _) = src("""{"error":"Bucket nope does not exist."}""")
            assertThrows(IllegalStateException::class.java) {
                runBlocking { errSource.rows("nope", listOf("a"), null, cacheKey = "k") }
            }
            val (okSource, _) = src("""{"bucket":[{"a":"1"}]}""")
            val row = okSource.rows("b", listOf("a"), null, cacheKey = "k2").single()
            assertThrows(IllegalStateException::class.java) { row.requireText("missing", "ctx") }
        }
}
