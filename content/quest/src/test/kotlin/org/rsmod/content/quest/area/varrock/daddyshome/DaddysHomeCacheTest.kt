package org.rsmod.content.quest.area.varrock.daddyshome

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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
class DaddysHomeCacheTest {

    @Test
    fun `the miniquest row matches the stages and rewards`() {
        val row = QuestRow.getRow("dbrow.miniquest_daddyshome".asRSCM())
        assertEquals(DaddysHomeQuest.Complete, row.endstate)
        assertEquals(0, row.questpoints)
        assertEquals(1, row.type)
        assertEquals("npc.con_contractor_varrock", row.startnpc.single().internalName)
        assertEquals(4000, row.statXpAwarded.single().component2())
    }

    @Test
    fun `the status varbit is Jagex's own and holds every stage`() {
        val status = varbit("varbit.daddyshome_status")
        assertTrue((1 shl (status.endBit - status.startBit + 1)) > DaddysHomeQuest.Complete)
        assertEquals(VarpLifetime.Perm, ServerCacheManager.getVarp(status.baseVar.id)!!.scope)
        for (furniture in Furniture.entries) {
            val type = varbit(furniture.varbit)
            assertEquals(status.baseVar.id, type.baseVar.id, furniture.varbit)
            assertEquals(1, type.endBit - type.startBit, furniture.varbit)
        }
    }

    @Test
    fun `the crate flag is a server-only varbit inside the allotted range`() {
        val varp = "varp.daddyshome_state".asRSCM(RSCMType.VARP)
        val flag = "varbit.daddyshome_crate_opened".asRSCM(RSCMType.VARBIT)
        assertTrue(varp in 64064..64079, varp.toString())
        assertTrue(flag in 64064..64079, flag.toString())
        assertEquals(VarpLifetime.Perm, ServerCacheManager.getVarp(varp)!!.scope)
        assertEquals(varp, varbit("varbit.daddyshome_crate_opened").baseVar.id)
    }

    @Test
    fun `each furniture multiloc walks through untouched, broken, hotspot and built`() {
        for (furniture in Furniture.entries) {
            val base = loc(furniture.loc)
            assertEquals(furniture.varbit.asRSCM(RSCMType.VARBIT), base.multiVarBit, furniture.loc)
            val op = if (furniture == Furniture.Carpet) 4 else 0
            val states =
                (0..3).map { checkNotNull(ServerCacheManager.getObject(base.multiLoc[it] and 0xFFFF)) }
            assertNull(states[Furniture.Untouched].actions.getOpOrNull(op), furniture.loc)
            assertNotNull(states[Furniture.Broken].actions.getOpOrNull(op), furniture.loc)
            assertEquals("Build", states[Furniture.Cleared].actions.getOpOrNull(op), furniture.loc)
            assertNull(states[Furniture.Built].actions.getOpOrNull(op), furniture.loc)
        }
    }

    @Test
    fun `the broken pieces offer to demolish or remove`() {
        val ops =
            mapOf(
                "loc.daddyshome_stool_broken_1op" to "Demolish",
                "loc.daddyshome_chair_broken_1op" to "Demolish",
                "loc.daddyshome_table_broken_1op" to "Demolish",
                "loc.daddyshome_bed_broken_1op" to "Remove",
                "loc.daddyshome_carpet_middle_broken_1op" to "Remove",
            )
        for ((name, expected) in ops) {
            val op = if (name.contains("carpet")) 4 else 0
            assertEquals(expected, loc(name).actions.getOpOrNull(op), name)
        }
    }

    @Test
    fun `Marlo, Yarlo and the Lumber Yard operator are placed where the script expects`() {
        val spawns = rawNpcSpawns()
        assertTrue("npc.con_contractor_varrock" to CoordGrid(3241, 3471, 0) in spawns)
        assertTrue("npc.daddyshome_daddy" to CoordGrid(3239, 3395, 0) in spawns)
        assertTrue("npc.poh_sawmill_opp" in spawns.map { it.first })
        val marlo = checkNotNull(ServerCacheManager.getNpc("npc.con_contractor_varrock".asRSCM(RSCMType.NPC)))
        assertEquals("varbit.con_contract_discussed".asRSCM(RSCMType.VARBIT), marlo.multiVarBit)
        val yarlo = checkNotNull(ServerCacheManager.getNpc("npc.daddyshome_daddy".asRSCM(RSCMType.NPC)))
        assertEquals("Talk-to", yarlo.actions.getOpOrNull(0))
    }

    @Test
    fun `a saw lies on the floor of the Estate Agent's house`() {
        val text = java.io.File(".data/raw-cache/map/objs/varrock.toml").readText()
        val saw = Regex("obj = \"obj.poh_saw\"\\s*\\r?\\ncoords = \"0_50_54_41_14\"")
        assertTrue(saw.containsMatchIn(text))
    }

    @Test
    fun `the house is furnished with the broken pieces and the crates`() {
        val at = placed.map { ServerCacheManager.getObject(it.id)!!.internalName to it.coords }
        val expected =
            mapOf(
                "loc.daddyshome_stool_1" to CoordGrid(3244, 3394, 0),
                "loc.daddyshome_stool_2" to CoordGrid(3239, 3394, 0),
                "loc.daddyshome_chair" to CoordGrid(3241, 3393, 0),
                "loc.daddyshome_table_1" to CoordGrid(3245, 3394, 0),
                "loc.daddyshome_table_2" to CoordGrid(3240, 3394, 0),
                "loc.daddyshome_bed" to CoordGrid(3241, 3397, 0),
                "loc.daddyshome_crates" to CoordGrid(3243, 3398, 0),
                "loc.daddyshome_carpet_middle" to CoordGrid(3239, 3395, 0),
            )
        for ((name, coords) in expected) {
            assertTrue(name to coords in at, "$name at $coords")
        }
    }

    @Test
    fun `the items the script hands out exist`() {
        val objs =
            listOf(
                DaddysHomeQuest.Crate,
                DaddysHomeQuest.WaxwoodLogs,
                DaddysHomeQuest.WaxwoodPlank,
                DaddysHomeQuest.Plank,
                DaddysHomeFurniture.Cloth,
                "obj.coins",
            ) +
                DaddysHomeFurniture.Hammers +
                DaddysHomeFurniture.Saws +
                DaddysHomeFurniture.Nails +
                DaddysHomeFurniture.CrateContents.map { it.first }
        for (obj in objs) {
            assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
        }
    }

    private fun varbit(name: String) =
        checkNotNull(ServerCacheManager.getVarbit(name.asRSCM(RSCMType.VARBIT)))

    private fun loc(name: String) = checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC)))

    private fun rawNpcSpawns(): List<Pair<String, CoordGrid>> {
        val pattern =
            Regex(
                "npc = \"(npc[.][a-z0-9_]+)\"\\s*\\r?\\n" +
                    "coords = \"(\\d+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)\""
            )
        val text = java.io.File(".data/raw-cache/map/npcs/varrock.toml").readText()
        return pattern
            .findAll(text)
            .map {
                val (name, level, mx, mz, lx, lz) = it.destructured
                val x = mx.toInt() * 64 + lx.toInt()
                val z = mz.toInt() * 64 + lz.toInt()
                name to CoordGrid(x, z, level.toInt())
            }
            .toList()
    }

    private data class Placed(val id: Int, val coords: CoordGrid)

    private companion object {
        val placed = mutableListOf<Placed>()
        lateinit var cache: dev.openrune.filesystem.Cache

        @JvmStatic
        @BeforeAll
        fun load() {
            cache = ServerCacheManager.init(240)
            val collision = CollisionFlagMap()
            for ((mx, mz) in listOf(50 to 53, 50 to 54)) {
                val square = MapSquareKey(mx, mz)
                val group = (square.x shl 8) or square.z
                val tileData = InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0)))
                val locData = InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1)))
                val tiles = MapTileDecoder.decode(tileData)
                val spawns = MapLocListDecoder.decode(locData)
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
