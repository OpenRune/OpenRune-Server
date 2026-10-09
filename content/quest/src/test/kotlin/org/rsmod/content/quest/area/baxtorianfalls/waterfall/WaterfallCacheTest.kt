package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpLifetime
import dev.openrune.types.varp.baseVar
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.QuestRow
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class WaterfallCacheTest {

    @Test
    fun `the quest row matches the stages and points`() {
        val row = QuestRow.getRow("dbrow.quest_waterfall".asRSCM())
        assertEquals(WaterfallQuest.COMPLETE, row.endstate)
        assertEquals(1, row.questpoints)
    }

    @Test
    fun `progress sits on the quest varp and the ids are in the range of this quest`() {
        val progress = varbit("varbit.waterfall_progress")
        assertEquals("varp.waterfall_quest".asRSCM(RSCMType.VARP), progress.baseVar.id)
        assertTrue((1 shl (progress.endBit - progress.startBit + 1)) > WaterfallQuest.COMPLETE)
        for (name in
            listOf(
                "varbit.waterfall_progress",
                "varbit.waterfall_heard_of_treasure",
                "varbit.waterfall_met_golrie",
                "varbit.waterfall_pillar_runes",
            )) {
            assertTrue(name.asRSCM(RSCMType.VARBIT) in 64400..64409, name)
        }
        assertTrue("varp.waterfall_state".asRSCM(RSCMType.VARP) in 64400..64409)
    }

    @Test
    fun `the flags share one permanent server-only varp without overlapping`() {
        val state = "varp.waterfall_state".asRSCM(RSCMType.VARP)
        assertEquals(VarpLifetime.Perm, ServerCacheManager.getVarp(state)!!.scope)
        val bits = mutableListOf<Int>()
        for (name in
            listOf(
                "varbit.waterfall_heard_of_treasure",
                "varbit.waterfall_met_golrie",
                "varbit.waterfall_pillar_runes",
            )) {
            val type = varbit(name)
            assertEquals(state, type.baseVar.id, name)
            bits += (type.startBit..type.endBit).toList()
        }
        assertEquals(bits.size, bits.distinct().size)
        val pillars = varbit("varbit.waterfall_pillar_runes")
        assertTrue(pillars.endBit - pillars.startBit + 1 >= WaterfallQuest.PILLAR_COUNT * 3)
    }

    @Test
    fun `no Jagex varbit shares the progress bits of the quest varp`() {
        val varp = "varp.waterfall_quest".asRSCM(RSCMType.VARP)
        val progress = varbit("varbit.waterfall_progress")
        for (id in 0..21000) {
            val other = ServerCacheManager.getVarbit(id) ?: continue
            if (id == progress.id || other.baseVar.id != varp) continue
            assertTrue(
                other.endBit < progress.startBit || other.startBit > progress.endBit,
                "varbit $id overlaps the progress bits",
            )
        }
    }

    @Test
    fun `the items and npcs exist with the ops the script binds`() {
        for (obj in
            listOf(
                WaterfallQuest.BOOK,
                WaterfallQuest.GOLRIE_KEY,
                WaterfallQuest.PEBBLE,
                WaterfallQuest.AMULET,
                WaterfallQuest.URN_FULL,
                WaterfallQuest.URN_EMPTY,
                WaterfallQuest.BAXTORIAN_KEY,
                WaterfallQuest.ROPE,
                "obj.diamond",
                "obj.gold_bar",
                "obj.mithril_seed",
            )) {
            checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))) { obj }
        }
        assertEquals(
            "Read",
            checkNotNull(ServerCacheManager.getItem(WaterfallQuest.BOOK.asRSCM(RSCMType.OBJ)))
                .interfaceOptions.getOrNull(0),
        )
        for (npc in
            listOf(
                WaterfallQuest.ALMERA_NPC,
                WaterfallQuest.HUDON_NPC,
                WaterfallQuest.GERALD_NPC,
                WaterfallQuest.HADLEY_NPC,
                WaterfallQuest.GOLRIE_NPC,
            )) {
            val type = checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC))) { npc }
            assertEquals("Talk-to", type.actions.getOpOrNull(0), npc)
        }
    }

    @Test
    fun `the scenery has the ops the script binds`() {
        val expected =
            mapOf(
                "loc.lograft_waterfall_quest" to "Board",
                "loc.waterfall_swim_point" to "Swim",
                "loc.crossing_rock_waterfall_quest" to "Swim to",
                "loc.overhanging_tree1_waterfall_quest" to "Climb",
                "loc.waterfall_ledge_door" to "Open",
                "loc.barrel_waterfall_quest" to "Get in",
                "loc.bookcase_waterfall_quest" to "Search",
                "loc.golrie_crate_waterfall_quest" to "Search",
                "loc.golrie_gate_waterfall_quest" to "Open",
                "loc.glarials_tombstone_waterfall_quest" to "Read",
                "loc.glarials_chest_closed_waterfall_quest" to "Open",
                "loc.glarials_tomb_waterfall_quest" to "Search",
                "loc.baxtorian_door_waterfall_quest" to "Open",
                "loc.baxtorian_crate_waterfall_quest" to "Search",
                "loc.baxtorian_door_2_waterfall_quest" to "Open",
                "loc.baxtorian_chalice_waterfall_quest" to "Take treasure",
            )
        for ((symbol, op) in expected) {
            assertEquals(op, loc(symbol).actions.getOpOrNull(0), symbol)
        }
        assertEquals("Search", loc("loc.glarials_chest_open_waterfall_quest").actions.getOpOrNull(0))
        assertEquals("Shut", loc("loc.glarials_chest_open_waterfall_quest").actions.getOpOrNull(1))
        assertEquals("Statue of Glarial", loc("loc.statue_queen_waterfall_quest").name)
        for (open in
            listOf("loc.baxtorian_door_2_open_waterfall_quest", "loc.baxtorian_chalice_waterfall_quest_ash")) {
            loc(open)
        }
        for (form in
            listOf(
                "loc.stonepillar_small_waterfall_quest",
                "loc.stonepillar_small_waterfall_quest_noop",
                "loc.stonepillar_small_waterfall_quest_op",
            )) {
            loc(form)
        }
    }

    @Test
    fun `the scenery stands where the script expects`() {
        assertPlaced("loc.lograft_waterfall_quest", CoordGrid(2509, 3493, 0))
        assertPlaced("loc.crossing_rock_waterfall_quest", CoordGrid(2512, 3468, 0))
        assertPlaced("loc.overhanging_tree1_waterfall_quest", CoordGrid(2512, 3465, 0))
        assertPlaced("loc.waterfall_ledge_door", CoordGrid(2511, 3464, 0))
        assertPlacedOnEitherLevel("loc.barrel_waterfall_quest", CoordGrid(2512, 3463, 0))
        assertPlaced("loc.bookcase_waterfall_quest", CoordGrid(2520, 3426, 1))
        assertPlaced("loc.glarials_tombstone_waterfall_quest", CoordGrid(2558, 3444, 0))
        assertPlaced("loc.golrie_crate_waterfall_quest", CoordGrid(2548, 9565, 0))
        assertPlaced("loc.golrie_gate_waterfall_quest", CoordGrid(2515, 9575, 0))
        assertPlaced("loc.glarials_chest_closed_waterfall_quest", CoordGrid(2530, 9844, 0))
        assertPlaced("loc.glarials_tomb_waterfall_quest", CoordGrid(2542, 9811, 0))
        assertPlaced("loc.baxtorian_door_waterfall_quest", CoordGrid(2575, 9861, 0))
        assertPlaced("loc.baxtorian_crate_waterfall_quest", CoordGrid(2589, 9888, 0))
        assertPlaced("loc.baxtorian_door_2_waterfall_quest", CoordGrid(2568, 9893, 0))
        assertPlaced("loc.baxtorian_door_2_waterfall_quest", CoordGrid(2566, 9901, 0))
        assertPlaced("loc.baxtorian_door_2_waterfall_quest", CoordGrid(2604, 9900, 0))
        assertPlaced("loc.baxtorian_door_2_waterfall_quest", CoordGrid(2606, 9892, 0))
        assertPlaced("loc.statue_queen_waterfall_quest", CoordGrid(2565, 9916, 0))
        assertPlaced("loc.statue_queen_waterfall_quest", CoordGrid(2603, 9915, 0))
        assertPlacedOnEitherLevel("loc.baxtorian_chalice_waterfall_quest", CoordGrid(2565, 9911, 0))
        assertPlaced("loc.baxtorian_chalice_waterfall_quest", CoordGrid(2603, 9910, 0))
    }

    @Test
    fun `the ordinary tourist centre bookcases are the generic ones the hook claims`() {
        assertEquals("Search", loc("loc.bookcase").actions.getOpOrNull(0))
        assertEquals("Search", loc("loc.bookcase2").actions.getOpOrNull(0))
        val tourist =
            listOf(
                "loc.bookcase" to CoordGrid(2517, 3424, 1),
                "loc.bookcase" to CoordGrid(2520, 3429, 1),
                "loc.bookcase2" to CoordGrid(2516, 3431, 1),
                "loc.bookcase2" to CoordGrid(2519, 3424, 1),
            )
        for ((symbol, at) in tourist) {
            assertPlaced(symbol, at)
        }
    }

    @Test
    fun `the six pillars of the tomb room stand where the rune bits expect them`() {
        for (x in listOf(2562, 2569)) {
            for (z in listOf(9910, 9912, 9914)) {
                assertPlaced("loc.stonepillar_small_waterfall_quest", CoordGrid(x, z, 0))
                assertPlaced("loc.stonepillar_small_waterfall_quest", CoordGrid(x + 38, z - 1, 0))
            }
        }
    }

    @Test
    fun `the raised copy of the tomb room is the real room shifted by the script offset`() {
        for ((symbol, real) in
            listOf(
                "loc.statue_queen_waterfall_quest" to CoordGrid(2565, 9916, 0),
                "loc.baxtorian_door_2_waterfall_quest" to CoordGrid(2566, 9901, 0),
                "loc.baxtorian_door_2_waterfall_quest" to CoordGrid(2568, 9893, 0),
            )) {
            assertPlaced(symbol, real)
            assertPlaced(symbol, real.translate(38, -1))
        }
    }

    @Test
    fun `the quest npcs are placed in the world`() {
        val spawns = rawSpawns()
        val expected =
            mapOf(
                WaterfallQuest.ALMERA_NPC to CoordGrid(2522, 3498, 0),
                WaterfallQuest.HUDON_NPC to CoordGrid(2511, 3484, 0),
                WaterfallQuest.GERALD_NPC to CoordGrid(2528, 3414, 0),
                WaterfallQuest.HADLEY_NPC to CoordGrid(2516, 3428, 0),
                WaterfallQuest.GOLRIE_NPC to CoordGrid(2515, 9581, 0),
            )
        for ((npc, at) in expected) {
            assertTrue(spawns.any { it.first == npc && it.second == at }, "$npc at $at")
        }
    }

    private fun varbit(name: String) =
        checkNotNull(ServerCacheManager.getVarbit(name.asRSCM(RSCMType.VARBIT))) { name }

    private fun assertPlaced(name: String, at: CoordGrid) {
        val id = name.asRSCM(RSCMType.LOC)
        assertTrue(placed.any { it.id == id && it.coords == at }, "$name is not at $at")
    }

    private fun assertPlacedOnEitherLevel(name: String, at: CoordGrid) {
        val id = name.asRSCM(RSCMType.LOC)
        assertTrue(
            placed.any { it.id == id && it.coords.x == at.x && it.coords.z == at.z },
            "$name is not at $at",
        )
    }

    private fun loc(name: String) =
        checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC))) { name }

    private fun rawSpawns(): List<Pair<String, CoordGrid>> {
        val dir =
            listOf("", "../../")
                .map { java.io.File("$it.data/raw-cache/map/npcs") }
                .first { it.isDirectory }
        val pattern =
            Regex(
                "npc = \"(npc[.][a-z0-9_]+)\"\\s*\\r?\\n" +
                    "coords = \"(\\d+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)\""
            )
        return dir.listFiles { f -> f.name.endsWith(".toml") }!!.flatMap { file ->
            pattern
                .findAll(file.readText())
                .map {
                    val (name, level, mx, mz, lx, lz) = it.destructured
                    val x = mx.toInt() * 64 + lx.toInt()
                    val z = mz.toInt() * 64 + lz.toInt()
                    name to CoordGrid(x, z, level.toInt())
                }
                .toList()
        }
    }

    private data class Placed(val id: Int, val coords: CoordGrid)

    private companion object {
        val SQUARES =
            listOf(
                MapSquareKey(39, 53),
                MapSquareKey(39, 54),
                MapSquareKey(39, 149),
                MapSquareKey(39, 153),
                MapSquareKey(39, 154),
                MapSquareKey(40, 53),
                MapSquareKey(40, 154),
            )

        val placed = mutableListOf<Placed>()
        val collision = CollisionFlagMap()
        lateinit var cache: dev.openrune.filesystem.Cache

        @JvmStatic
        @BeforeAll
        fun load() {
            cache = ServerCacheManager.init(240)
            for (square in SQUARES) {
                val group = (square.x shl 8) or square.z
                val tileBytes = cache.data(MAPS, group, 0) ?: continue
                val locBytes = cache.data(MAPS, group, 1) ?: continue
                val tiles = MapTileDecoder.decode(InlineByteBuf(tileBytes))
                val spawns = MapLocListDecoder.decode(InlineByteBuf(locBytes))
                for (level in 0..3) for (x in square.x * 64 until square.x * 64 + 64 step 8) {
                    for (z in square.z * 64 until square.z * 64 + 64 step 8) {
                        collision.allocateIfAbsent(x, z, level)
                    }
                }
                val builder = GameMapBuilder()
                GameMapDecoder.putMaps(collision, square, tiles)
                GameMapDecoder.putLocs(builder, collision, square, tiles, spawns)
                for ((packed, zone) in builder.zoneBuilders) {
                    val base = ZoneKey(packed).toCoords()
                    for (entry in zone.build().byte2IntEntrySet()) {
                        val key = LocZoneKey(entry.byteKey)
                        val loc = LocEntity(entry.intValue)
                        placed += Placed(loc.id, base.translate(key.x, key.z))
                    }
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun close() {
            cache.close()
        }
    }
}
