package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
class ZulrahCacheEvidenceTest {
    @Test
    fun `provisional stun presentation uses native player and spot animation symbols`() {
        assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
        ServerCacheManager.init(240)
        assertEquals(848, "seq.human_stunned".asRSCM(RSCMType.SEQ))
        assertEquals(80, "spotanim.stunned".asRSCM(RSCMType.SPOTANIM))
        assertEquals(706, "seq.stunned".asRSCM(RSCMType.SEQ))
    }

    @Test
    fun `village teleport and recorded island landing tiles are walkable in local cache`() {
        assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
        val cache = ServerCacheManager.init(240)
        val collision = CollisionFlagMap()
        val builder = GameMapBuilder()
        val regions = listOf(8751, 9007, 9008).map { region ->
            val square = MapSquareKey(region)
            val tiles = MapTileDecoder.decode(
                InlineByteBuf(requireNotNull(cache.data(MAPS, region, 0))),
            )
            val locs = MapLocListDecoder.decode(
                InlineByteBuf(requireNotNull(cache.data(MAPS, region, 1))),
            )
            for (level in 0..3) {
                for (x in 0..63 step 8) {
                    for (z in 0..63 step 8) {
                        collision.allocateIfAbsent(square.x * 64 + x, square.z * 64 + z, level)
                    }
                }
            }
            Triple(square, tiles, locs)
        }
        regions.forEach { (square, tiles, _) -> GameMapDecoder.putMaps(collision, square, tiles) }
        regions.forEach { (square, tiles, locs) ->
            GameMapDecoder.putLocs(builder, collision, square, tiles, locs)
        }
        // Submission 3276: rebuild 8314 / arrival 8314; east-side loot and exit at 9388.
        val landingTiles = listOf(
            ZulrahIsland.zulAndraTeleport,
            CoordGrid(2268, 3068, 0), CoordGrid(2274, 3072, 0), CoordGrid(2273, 3072, 0),
        )
        val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR
        for (tile in landingTiles) {
            assertEquals(0, collision[tile.x, tile.z, tile.level] and blocked, "$tile")
        }
        val sight = org.rsmod.api.route.RayCastValidator(collision)
        for (tile in landingTiles.drop(1)) {
            assertTrue((-1..1).any { x -> (-1..1).any { z ->
                (x != 0 || z != 0) && sight.hasLineOfWalk(tile, tile.translate(x, z))
            } }, "Expected a clear neighbouring exit tile at $tile")
        }
        val southBoss = CoordGrid(2266, 3062, 0)
        val eastPlayer = CoordGrid(2272, 3072, 0)
        assertFalse(sight.hasLineOfSight(southBoss, eastPlayer, 5, 5))
        assertTrue(sight.hasLineOfSight(eastPlayer, southBoss, destWidth = 5, destLength = 5))
    }

    @Test
    fun `boat display forms match the original transforming map object`() {
        assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
        ServerCacheManager.init(240)
        val base = requireNotNull(ServerCacheManager.getObject("loc.snakeboss_boat".asRSCM(RSCMType.LOC)))
        val board = "loc.snakeboss_boat_1op".asRSCM(RSCMType.LOC)
        val repeat = "loc.snakeboss_boat_2ops".asRSCM(RSCMType.LOC)
        assertEquals(10068, base.id)
        assertEquals(46241, board)
        assertEquals(46242, repeat)
        assertEquals(listOf(board, board, board, repeat), base.multiLoc.toList())
        assertEquals(repeat, base.multiDefault)
        assertEquals("Sacrificial boat", ServerCacheManager.getObject(board)?.name)
    }

    @Test
    fun `teleport scroll and cloud footprint match cached OSRS definitions`() {
        assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
        ServerCacheManager.init(240)
        val scroll = requireNotNull(ServerCacheManager.getItem(ZulAndraTeleportScript.SCROLL.asRSCM(RSCMType.OBJ)))
        assertEquals(12938, scroll.id)
        assertEquals("Zul-andra teleport", scroll.name)
        assertEquals(3864, "seq.teleport_scroll_open".asRSCM(RSCMType.SEQ))
        assertEquals(1039, "spotanim.telescroll_teleport".asRSCM(RSCMType.SPOTANIM))
        assertEquals(200, "synth.teleport_all".asRSCM(RSCMType.SYNTH))
        val cloud = requireNotNull(ServerCacheManager.getObject("loc.snakeboss_poisoncloud".asRSCM(RSCMType.LOC)))
        assertEquals(3, cloud.width)
        assertEquals(3, cloud.length)
    }

    @Test
    fun `minion stats and combat symbols resolve from the local definitions`() {
        assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
        ServerCacheManager.init(240)
        for (symbol in listOf("npc.snakeboss_minion_melee", "npc.snakeboss_minion_magic")) {
            val npc = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
            assertEquals(1, npc.hitpoints)
            assertEquals(1, npc.size)
            assertEquals(3, npc.paramsRaw?.get(14))
            assertEquals(if (npc.id == 2045) 140 else 1, npc.attack)
            assertEquals(if (npc.id == 2046) 185 else 1, npc.magic)
            assertEquals(if (npc.id == 2045) 120 else 0, npc.param(org.rsmod.api.config.refs.params.attack_melee),
                "Rebuild the server cache with ZulrahPluginPack before running combat tests")
        }
        val events = ZulrahRoutine.recorded.events
        for (event in events) {
            if (event.kind == "emerge") assertNotNull(ServerCacheManager.getNpc(event.symbol.asRSCM(RSCMType.NPC)))
            if (event.kind == "egg") assertNotNull(ServerCacheManager.getNpc(event.spawn.asRSCM(RSCMType.NPC)))
        }
        assertEquals(2408, "seq.snakeboss_pet_death".asRSCM(RSCMType.SEQ))
        assertEquals(1230, "spotanim.snakeboss_minion_spell".asRSCM(RSCMType.SPOTANIM))
    }

    @Test
    fun `local forms match OpenRune revision 240 diff definitions`() {
        assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
        ServerCacheManager.init(240)
        assertEquals(dev.openrune.types.varp.VarpLifetime.Perm,
            ServerCacheManager.getVarp("varp.total_snakeboss_kills".asRSCM(RSCMType.VARP))?.scope)
        assertEquals(1518, org.rsmod.api.table.CollectionLogCategoriesRow.all()
            .single { it.structId == 505 }.countVarp1)
        val symbols = listOf(
            "npc.snakeboss_boss_ranged", "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic",
        )
        symbols.forEachIndexed { index, symbol ->
            val npc = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
            assertEquals(2042 + index, npc.id, symbol)
            assertEquals("Zulrah", npc.name, symbol)
            assertEquals(5, npc.size, symbol)
            assertEquals(725, npc.combatLevel, symbol)
            assertEquals(500, npc.hitpoints, symbol)
            assertEquals(300, npc.defence, symbol)
            assertEquals(300, npc.ranged, symbol)
            assertEquals(300, npc.magic, symbol)
            assertEquals(listOf(-45, 0, 300)[index], npc.paramsRaw?.get(8), symbol)
        }
    }
}
