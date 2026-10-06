package org.rsmod.content.quest.area.alkharid.princealirescue

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.loc.MapLocDefinition
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.npc.MapNpcDefinition
import dev.openrune.map.npc.MapNpcListDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.varp.baseVar
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.config.refs.params
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.CellDoor
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcJoe
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcKeli
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcPrinceCell
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcPrincePalace
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.QuestKey
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageAliEscaped
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageComplete
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageJoeDrunk
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageKeliTied
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey

/**
 * Pins the cache facts the quest is written against: the quest row, the `varp.princequest`
 * multinpcs that hide Keli, Joe and the Prince as the rescue goes on, the toll gate's multilocs and
 * where its leaves stand, where the cell gate and its occupants stand, Osman staying put, the
 * palace doors, and the server-only progress varbits.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PrinceAliRescueCacheTest {

    @Test
    fun `the quest row matches the stages the script uses`() {
        val row = QuestRow.getRow("dbrow.$QuestKey".asRSCM())
        assertEquals(StageComplete, row.endstate)
        assertEquals(3, row.questpoints)
    }

    @Test
    fun `the jail multinpcs follow the stage`() {
        assertShownUntil("npc.lady_keli", NpcKeli, StageJoeDrunk, StageKeliTied)
        assertShownUntil("npc.prince_ali_prison", NpcPrinceCell, StageKeliTied, StageAliEscaped)
        assertShownUntil("npc.joe", NpcJoe, StageAliEscaped, StageComplete)
        val palace = npc("npc.prince_ali_palace")
        assertEquals(NpcPrincePalace.asRSCM(RSCMType.NPC), palace.transforms!![StageAliEscaped])
        assertEquals(-1, palace.transforms!![0])
    }

    @Test
    fun `the cell gate separates the prince from his guard`() {
        assertLocAt(CellDoor, CoordGrid(3123, 3243, 0))
        assertNpcAt("npc.prince_ali_prison", CoordGrid(3123, 3242, 0))
        assertNpcAt("npc.joe", CoordGrid(3123, 3245, 0))
        assertNpcAt("npc.lady_keli", CoordGrid(3128, 3244, 0))
    }

    @Test
    fun `joe and keli are spawned once as multinpcs with no always-visible duplicate`() {
        assertNoNpcAt("npc.joe_vis", CoordGrid(3124, 3244, 0))
        assertNoNpcAt("npc.lady_keli_vis", CoordGrid(3128, 3244, 0))
    }

    @Test
    fun `the toll gate leaves lose their toll option when the quest is complete`() {
        for ((leaf, y) in listOf("loc.kharidmetalgateclosedl" to 3227, "loc.kharidmetalgateclosedr" to 3228)) {
            val type = loc(leaf)
            assertEquals("varp.princequest".asRSCM(RSCMType.VARP), type.multiVarp)
            val side = leaf.removePrefix("loc.kharidmetalgateclosed")
            val toll = loc("loc.kharidmetalgateclosed${side}_2op")
            val free = loc("loc.kharidmetalgateclosed${side}_1op")
            for (stage in 0 until StageAliEscaped) {
                assertEquals(toll.id, type.multiLoc[stage] and 0xFFFF, "$leaf at stage $stage")
            }
            assertEquals(free.id, type.multiLoc[StageAliEscaped] and 0xFFFF)
            assertEquals(free.id, type.multiDefault)
            assertEquals("Open", toll.actions.getOpOrNull(0))
            assertEquals("Pay-toll(10gp)", toll.actions.getOpOrNull(3))
            assertEquals("Open", free.actions.getOpOrNull(0))
            assertEquals(null, free.actions.getOpOrNull(3))
            assertLocAt(leaf, CoordGrid(3268, y, 0))
        }
        for (open in listOf("loc.inacmetalgateopenl", "loc.inacmetalgateopenr")) {
            loc(open)
        }
    }

    @Test
    fun `the border guards only talk`() {
        for (guard in listOf("npc.borderguard1", "npc.borderguard2")) {
            val type = npc(guard)
            assertEquals("Talk-to", type.actions.getOpOrNull(0))
            assertEquals(null, type.actions.getOpOrNull(1))
            assertEquals(null, type.actions.getOpOrNull(2))
        }
    }

    @Test
    fun `osman stands still`() {
        for (osman in listOf("npc.osman", "npc.contact_osman_multi")) {
            val type = npc(osman)
            assertEquals(MoveRestrict.NoMove, type.moveRestrict, osman)
            assertEquals(0, type.wanderRange, osman)
            assertEquals(NpcMode.None, type.defaultMode, osman)
        }
    }

    @Test
    fun `the palace doors are double doors`() {
        val left = loc("loc.bankdoor_l")
        val right = loc("loc.bankdoor_r")
        assertEquals("content.closed_left_door".asRSCM(RSCMType.CONTENT), left.contentGroup)
        assertEquals("content.closed_right_door".asRSCM(RSCMType.CONTENT), right.contentGroup)
        assertEquals("content.opened_left_door".asRSCM(RSCMType.CONTENT), loc("loc.openbankdoor_l").contentGroup)
        assertEquals("content.opened_right_door".asRSCM(RSCMType.CONTENT), loc("loc.openbankdoor_r").contentGroup)
        assertEquals("loc.openbankdoor_l".asRSCM(RSCMType.LOC), left.param(params.next_loc_stage).id)
        assertEquals("loc.openbankdoor_r".asRSCM(RSCMType.LOC), right.param(params.next_loc_stage).id)
        assertLocAt("loc.bankdoor_l", CoordGrid(3287, 3171, 0))
        assertLocAt("loc.bankdoor_r", CoordGrid(3287, 3172, 0))
    }

    @Test
    fun `the progress varbit sits on the quest varp and the flags share one server varp`() {
        val progress = checkNotNull(ServerCacheManager.getVarbit("varbit.prince_ali_progress".asRSCM(RSCMType.VARBIT)))
        assertEquals("varp.princequest".asRSCM(RSCMType.VARP), progress.baseVar.id)
        assertTrue((1 shl (progress.endBit - progress.startBit + 1)) > StageComplete)

        val state = "varp.prince_ali_state".asRSCM(RSCMType.VARP)
        for (name in
            listOf(
                "varbit.prince_ali_keli_asked",
                "varbit.prince_ali_key_ordered",
                "varbit.prince_ali_key_obtained",
                "varbit.prince_ali_met_leela",
                "varbit.prince_ali_joe_beer",
            )) {
            val varbit = checkNotNull(ServerCacheManager.getVarbit(name.asRSCM(RSCMType.VARBIT))) { name }
            assertEquals(state, varbit.baseVar.id, name)
        }
        assertFalse(checkNotNull(ServerCacheManager.getVarp(state)).transmit.name == "Always")
    }

    private fun assertShownUntil(base: String, vis: String, lastShown: Int, firstHidden: Int) {
        val type = npc(base)
        assertEquals("varp.princequest".asRSCM(RSCMType.VARP), type.multiVarp)
        val transforms = checkNotNull(type.transforms)
        assertEquals(vis.asRSCM(RSCMType.NPC), transforms[0])
        assertEquals(vis.asRSCM(RSCMType.NPC), transforms[lastShown])
        assertTrue(firstHidden >= transforms.size || transforms[firstHidden] == -1, "$base at $firstHidden")
    }

    private fun npc(name: String) =
        checkNotNull(ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))) { "$name missing" }

    private fun loc(name: String) =
        checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC))) { "$name missing" }

    private fun assertLocAt(loc: String, coords: CoordGrid) {
        val id = loc.asRSCM(RSCMType.LOC)
        assertTrue(locSpawns(coords).any { it.first == id && it.second == coords }, "$loc is not at $coords")
    }

    private fun locSpawns(coords: CoordGrid): List<Pair<Int, CoordGrid>> {
        val square = MapSquareKey.from(coords)
        val data = checkNotNull(cache.data(MAPS, square.id, 1)) { "no locs in ${square.id}" }
        return MapLocListDecoder.decode(InlineByteBuf(data)).spawns.map(::MapLocDefinition).map {
            it.id to square.toCoords(it.level).translate(it.localX, it.localZ)
        }
    }

    private fun npcSpawns(coords: CoordGrid): List<Pair<Int, CoordGrid>> {
        val square = MapSquareKey.from(coords)
        val data = checkNotNull(cache.data(MAPS, square.id, 5)) { "no npcs in ${square.id}" }
        return MapNpcListDecoder.decode(InlineByteBuf(data)).packedSpawns.map(::MapNpcDefinition).map {
            it.id to square.toCoords(it.level).translate(it.localX, it.localZ)
        }
    }

    private fun assertNpcAt(npc: String, coords: CoordGrid) {
        val id = npc.asRSCM(RSCMType.NPC)
        assertTrue(npcSpawns(coords).any { it.first == id && it.second == coords }, "$npc is not at $coords")
    }

    private fun assertNoNpcAt(npc: String, coords: CoordGrid) {
        val id = npc.asRSCM(RSCMType.NPC)
        assertFalse(npcSpawns(coords).any { it.first == id }, "$npc is still spawned in ${MapSquareKey.from(coords)}")
    }

    private companion object {
        lateinit var cache: dev.openrune.filesystem.Cache

        @JvmStatic
        @BeforeAll
        fun loadCache() {
            cache = ServerCacheManager.init(240)
        }

        @JvmStatic
        @AfterAll
        fun closeCache() {
            cache.close()
        }
    }
}
