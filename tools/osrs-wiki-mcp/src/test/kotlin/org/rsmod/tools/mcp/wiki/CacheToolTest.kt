package org.rsmod.tools.mcp.wiki

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheToolTest {

    private val factory = CacheTool()

    private fun hit(
        type: CacheSearchType,
        id: Int,
        name: String,
        summary: String = "",
        data: String = "",
    ): CacheTool.IndexedHit = factory.indexed(type, id, name, summary, data)

    private fun snapshot(
        revision: Int = 1,
        vararg entries: Pair<CacheSearchType, List<CacheTool.IndexedHit>>,
    ): CacheTool.Snapshot = CacheTool.Snapshot(revision = revision, byType = mapOf(*entries))

    private fun search(
        snap: CacheTool.Snapshot,
        cacheKind: CacheKind = CacheKind.LIVE,
        type: CacheSearchType,
        query: String? = null,
        id: Int? = null,
        limit: Int = 25,
    ) = factory.searchSnapshot(snap, cacheKind, type, query, id, limit)

    // ---- query filtering ----

    @Test
    fun `search by name returns matching hits`() {
        val snap = snapshot(
            entries = arrayOf(
                CacheSearchType.Npc to listOf(
                    hit(CacheSearchType.Npc, 1, "Black demon"),
                    hit(CacheSearchType.Npc, 2, "Lesser demon"),
                )
            )
        )

        val result = search(snap, type = CacheSearchType.Npc, query = "black demon")

        assertEquals(1, result.totalMatches)
        assertEquals("Black demon", result.matches.first().name)
    }

    @Test
    fun `search is case-insensitive`() {
        val snap = snapshot(
            entries = arrayOf(
                CacheSearchType.Npc to listOf(hit(CacheSearchType.Npc, 1, "Black Demon"))
            )
        )

        val result = search(snap, type = CacheSearchType.Npc, query = "BLACK DEMON")
        assertEquals(1, result.totalMatches)
    }

    // ---- id filtering ----

    @Test
    fun `search by id returns exact match only`() {
        val snap = snapshot(
            entries = arrayOf(
                CacheSearchType.Npc to listOf(
                    hit(CacheSearchType.Npc, 100, "Goblin"),
                    hit(CacheSearchType.Npc, 101, "Goblin"),
                    hit(CacheSearchType.Npc, 200, "Guard"),
                )
            )
        )

        val result = search(snap, type = CacheSearchType.Npc, id = 101)
        assertEquals(1, result.totalMatches)
        assertEquals(101, result.matches.first().id)
    }

    // ---- type=all ----

    @Test
    fun `search type all spans all type buckets`() {
        val snap = snapshot(
            entries = arrayOf(
                CacheSearchType.Npc to listOf(hit(CacheSearchType.Npc, 1, "Black demon")),
                CacheSearchType.Obj to listOf(hit(CacheSearchType.Obj, 2, "Black shield")),
            )
        )

        val result = search(snap, type = CacheSearchType.All, query = "black")
        assertEquals(2, result.totalMatches)
    }

    @Test
    fun `search specific type excludes other buckets`() {
        val snap = snapshot(
            entries = arrayOf(
                CacheSearchType.Npc to listOf(hit(CacheSearchType.Npc, 1, "Black demon")),
                CacheSearchType.Obj to listOf(hit(CacheSearchType.Obj, 2, "Black shield")),
            )
        )

        val result = search(snap, type = CacheSearchType.Npc, query = "black")
        assertEquals(1, result.totalMatches)
        assertEquals("npc", result.matches.first().type)
    }

    // ---- limit / truncation ----

    @Test
    fun `result truncated when matches exceed limit`() {
        val snap = snapshot(
            entries = arrayOf(CacheSearchType.Npc to (1..5).map { hit(CacheSearchType.Npc, it, "Goblin $it") })
        )

        val result = search(snap, type = CacheSearchType.Npc, limit = 3)
        assertEquals(5, result.totalMatches)
        assertEquals(3, result.matches.size)
        assertTrue(result.truncated)
    }

    @Test
    fun `result not truncated when matches within limit`() {
        val snap = snapshot(
            entries = arrayOf(CacheSearchType.Npc to (1..3).map { hit(CacheSearchType.Npc, it, "Goblin $it") })
        )

        val result = search(snap, type = CacheSearchType.Npc, limit = 10)
        assertEquals(3, result.totalMatches)
        assertFalse(result.truncated)
    }

    // ---- empty results ----

    @Test
    fun `search with no matches returns empty result`() {
        val snap = snapshot(
            entries = arrayOf(CacheSearchType.Npc to listOf(hit(CacheSearchType.Npc, 1, "Goblin")))
        )

        val result = search(snap, type = CacheSearchType.Npc, query = "dragon")
        assertEquals(0, result.totalMatches)
        assertTrue(result.matches.isEmpty())
    }

    // ---- searchBlob does not include data ----

    @Test
    fun `search does not match on data field content`() {
        val snap = snapshot(
            entries = arrayOf(
                // name is "Goblin" but data contains "dragon_keyword"
                CacheSearchType.Npc to listOf(hit(CacheSearchType.Npc, 1, "Goblin", data = "dragon_keyword=123"))
            )
        )

        val result = search(snap, type = CacheSearchType.Npc, query = "dragon_keyword")
        assertEquals(0, result.totalMatches, "data field should not be searched")
    }
}
