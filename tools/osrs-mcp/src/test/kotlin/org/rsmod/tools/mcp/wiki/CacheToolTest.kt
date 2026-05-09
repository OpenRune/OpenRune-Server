package org.rsmod.tools.mcp.wiki

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheToolTest {
    private val tool = CacheTool()

    @Test
    fun `search snapshot filters by query over indexed blob`() {
        val snapshot =
            CacheTool.Snapshot(
                revision = 1,
                byType =
                    mapOf(
                        CacheSearchType.Npc to listOf(tool.indexed(CacheSearchType.Npc, 240, "Black demon", "combat=172", "models=[1,2,3]")),
                        CacheSearchType.Item to listOf(tool.indexed(CacheSearchType.Item, 100, "Rune sword", "cost=20000", "stackable=false")),
                    ),
            )

        val result =
            tool.searchSnapshot(
                snapshot = snapshot,
                cacheKind = CacheKind.LIVE,
                type = CacheSearchType.All,
                query = "black demon",
                id = null,
                limit = 25,
            )

        assertEquals(1, result.totalMatches)
        assertEquals(240, result.matches.first().id)
    }

    @Test
    fun `search snapshot excludes data from query matching`() {
        val snapshot = snapshotOf(tool.indexed(CacheSearchType.Npc, 240, "Black demon", "combat=172", "models=[999999]"))

        val result =
            tool.searchSnapshot(
                snapshot = snapshot,
                cacheKind = CacheKind.LIVE,
                type = CacheSearchType.All,
                query = "999999",
                id = null,
                limit = 25,
            )

        assertEquals(0, result.totalMatches)
        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun `search snapshot filters by id`() {
        val snapshot =
            snapshotOf(
                tool.indexed(CacheSearchType.Npc, 240, "Black demon", "combat=172", "data"),
                tool.indexed(CacheSearchType.Npc, 2048, "Black demon", "combat=172", "data"),
            )

        val result = tool.searchSnapshot(snapshot, CacheKind.LIVE, CacheSearchType.Npc, null, 2048, 25)

        assertEquals(1, result.totalMatches)
        assertEquals(2048, result.matches.first().id)
    }

    @Test
    fun `search snapshot respects type filter`() {
        val npc = tool.indexed(CacheSearchType.Npc, 240, "Black demon", "combat=172", "data")
        val item = tool.indexed(CacheSearchType.Item, 100, "Rune sword", "cost=20000", "data")
        val snapshot =
            CacheTool.Snapshot(
                revision = 1,
                byType = mapOf(CacheSearchType.Npc to listOf(npc), CacheSearchType.Item to listOf(item)),
            )

        val npcOnly = tool.searchSnapshot(snapshot, CacheKind.LIVE, CacheSearchType.Npc, null, null, 25)
        val all = tool.searchSnapshot(snapshot, CacheKind.LIVE, CacheSearchType.All, null, null, 25)

        assertEquals(1, npcOnly.totalMatches)
        assertEquals(2, all.totalMatches)
    }

    @Test
    fun `search snapshot clamps limit and sets truncated`() {
        val snapshot =
            snapshotOf(
                tool.indexed(CacheSearchType.Npc, 3, "n3", "s", "d"),
                tool.indexed(CacheSearchType.Npc, 1, "n1", "s", "d"),
                tool.indexed(CacheSearchType.Npc, 2, "n2", "s", "d"),
            )

        val result = tool.searchSnapshot(snapshot, CacheKind.LIVE, CacheSearchType.Npc, null, null, 1)

        assertEquals(3, result.totalMatches)
        assertEquals(1, result.matches.size)
        assertTrue(result.truncated)
        assertEquals(1, result.matches.first().id)
    }

    @Test
    fun `search snapshot not truncated when limit exceeds matches`() {
        val snapshot = snapshotOf(tool.indexed(CacheSearchType.Npc, 10, "n", "s", "d"))

        val result = tool.searchSnapshot(snapshot, CacheKind.LIVE, CacheSearchType.Npc, null, null, 1000)

        assertEquals(1, result.totalMatches)
        assertEquals(1, result.matches.size)
        assertFalse(result.truncated)
    }

    private fun snapshotOf(vararg hits: CacheTool.IndexedHit): CacheTool.Snapshot =
        CacheTool.Snapshot(revision = 1, byType = mapOf(CacheSearchType.Npc to hits.toList()))
}
