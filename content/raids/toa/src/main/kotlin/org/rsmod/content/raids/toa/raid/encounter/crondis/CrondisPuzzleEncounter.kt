package org.rsmod.content.raids.toa.raid.encounter.crondis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.obj.Obj
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * The Crondis puzzle, "Test of Resourcefulness". Port of Offline_Scape CrondisPuzzleEncounter.
 *
 * Players take water containers, fill them at the four waterfalls and water the Palm of
 * Resourcefulness. The palm needs [WATER_PER_PLAYER] per player; it grows a stage every 25%
 * (npcs toa_crondis_tree_1..5), and at the fifth stage the room is complete and the end barrier
 * opens.
 *
 * This file is the room's state; the ops (take, fill, water, check, empty) are in
 * [CrondisPuzzleScript].
 *
 * Not ported yet (v20): acid trails, the spear statues, the crocodiles that drain the palm, and
 * the palm's overhead progress hitbar (Offline_Scape used hitbar type 11). Also the seq preloads
 * (client script 1846) sent on entry.
 */
class CrondisPuzzleEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    private var palm: Npc? = null
    private var palmStage = 0

    /** Water poured onto the palm so far (Offline_Scape: the palm's missing hitpoints). */
    var water = 0
        private set

    /** Water needed to finish (Offline_Scape: the palm's max hitpoints, teamSize * 200). */
    val goal: Int
        get() = teamSize * WATER_PER_PLAYER

    /** The two containers lying in the room; taking one leaves it there (a free supply). */
    private val floorContainers = ArrayList<Obj>()

    // ---- Room lifecycle ----

    override fun onBuilt() {
        for (tile in CONTAINER_TILES) {
            floorContainers += deps.objRepo.add(CONTAINER, coords(tile), Int.MAX_VALUE)
        }
        for (tile in STATUE_SOUTH_TILES) addLoc(STATUE, tile, LocAngle.West)
        for (tile in STATUE_NORTH_TILES) addLoc(STATUE, tile, LocAngle.East)
        addLoc(PALM_BLOCKER, PALM_TILE, LocAngle.West)
        spawnPalm(stage = 0)
    }

    override fun onStart() {
        water = 0
        for (player in players) openBar(player)
    }

    /** Someone joining a running challenge gets the bar too. */
    override fun onEnter(player: Player) {
        if (stage == ToaStage.STARTED) openBar(player)
    }

    override fun onLeave(player: Player) {
        closeBar(player)
        removeContainers(player)
    }

    override fun onComplete() {
        for (player in players) {
            closeBar(player)
            removeContainers(player)
        }
        for (obj in floorContainers) deps.objRepo.del(obj, Int.MAX_VALUE)
        floorContainers.clear()
        restoreWaterfalls()
        removeEndBarrier()
    }

    override fun onReset() {
        for (player in players) {
            closeBar(player)
            removeContainers(player)
        }
        water = 0
        spawnPalm(stage = 0)
        restoreWaterfalls()
    }

    // ---- The palm ----

    /**
     * Adds [amount] water to the palm (Offline_Scape: a "shield charge" hit on the palm). Grows
     * the palm when it crosses a 25% step; the last step completes the room.
     */
    fun waterPalm(amount: Int) {
        if (stage != ToaStage.STARTED || amount <= 0) return
        water = (water + amount).coerceAtMost(goal)
        for (player in players) updateBar(player)

        // Offline_Scape getNpcId: stage i while water < goal * (i + 1) / 4.
        val newStage = (water * FINAL_STAGE / goal).coerceAtMost(FINAL_STAGE)
        if (newStage == palmStage) return
        spawnPalm(newStage)
        if (newStage == FINAL_STAGE) {
            complete()
        } else {
            // The new palm is a new npc, so the bar is re-pointed at it.
            for (player in players) openBar(player)
        }
    }

    /**
     * The palm stages are separate npc types, so a stage change replaces the npc (Offline_Scape
     * transformed it). TODO: sounds 6516 (grow) / 6529 (shrink, crocodiles) to the room.
     */
    private fun spawnPalm(stage: Int) {
        palm?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        val npc = Npc(PALM_STAGES[stage], coords(PALM_TILE))
        npc.respawnDir = Direction.South
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        palm = npc
        palmStage = stage
    }

    // ---- Progress bar (Offline_Scape HpHud: 0 -> goal as the palm is watered) ----

    private fun openBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onOpen(player, npc)
        updateBar(player)
    }

    private fun updateBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onUpdate(player, npc, currentHp = water, maxHp = goal)
    }

    private fun closeBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onClose(player, npc, instant = true)
    }

    // ---- Waterfalls ----

    /**
     * A filled-from waterfall runs dry for a while: the empty variant is added with a duration,
     * and the map's own waterfall comes back when it expires. Offline_Scape: 128 ticks, 18 fewer
     * per extra party member.
     */
    fun drainWaterfall(waterfall: BoundLocInfo) {
        val refillTicks = (BASE_REFILL_TICKS - (teamSize - 1) * REFILL_TICKS_PER_PLAYER).coerceAtLeast(1)
        deps.locRepo.change(waterfall, WATER_SOURCE_EMPTY, refillTicks)
    }

    /** Offline_Scape spawnWaterfalls: every waterfall full again (end of room, or a reset). */
    private fun restoreWaterfalls() {
        for (tile in WATERFALL_SOUTH_TILES) addLoc(WATER_SOURCE, tile, LocAngle.West)
        for (tile in WATERFALL_NORTH_TILES) addLoc(WATER_SOURCE, tile, LocAngle.East)
    }

    // ---- Helpers ----

    /** Offline_Scape onRoomEnd: the three barrier tiles on the west side, (3922, 5279..5281). */
    private fun removeEndBarrier() {
        val barrier = ServerCacheManager.getObject(BARRIER.asRSCM(RSCMType.LOC)) ?: return
        for (dz in 0 until END_BARRIER_LENGTH) {
            val loc = deps.locRepo.findExact(coords(END_BARRIER_TILE.translate(0, dz)), barrier) ?: continue
            deps.locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun addLoc(type: String, static: CoordGrid, angle: LocAngle) {
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
    }

    companion object {
        const val CONTAINER = "obj.toa_crondis_water_container"
        const val WATER_SOURCE = "loc.toa_crondis_water_source"
        const val WATER_SOURCE_EMPTY = "loc.toa_crondis_water_source_empty"

        /** The four stages that can still be watered (op1 "Water"); stage 5 has no ops. */
        val WATERABLE_PALMS =
            listOf(
                "npc.toa_crondis_tree_1",
                "npc.toa_crondis_tree_2",
                "npc.toa_crondis_tree_3",
                "npc.toa_crondis_tree_4",
            )
        private val PALM_STAGES = WATERABLE_PALMS + "npc.toa_crondis_tree_5"
        private const val FINAL_STAGE = 4

        /** Container capacity, stored in the obj's vars as a percentage (Offline_Scape charges). */
        const val CONTAINER_FULL = 100

        private const val WATER_PER_PLAYER = 200
        private const val BASE_REFILL_TICKS = 128
        private const val REFILL_TICKS_PER_PLAYER = 18

        private const val STATUE = "loc.toa_crondis_column_trap"
        private const val PALM_BLOCKER = "loc.invisible_type8_blocking_size5"
        private const val BARRIER = "loc.toa_path_barrier"

        private val CONTAINER_TILES = listOf(CoordGrid(3934, 5273, 0), CoordGrid(3938, 5287, 0))
        private val STATUE_SOUTH_TILES = listOf(CoordGrid(3943, 5255, 0), CoordGrid(3929, 5255, 0))
        private val STATUE_NORTH_TILES = listOf(CoordGrid(3929, 5304, 0), CoordGrid(3943, 5304, 0))
        private val WATERFALL_SOUTH_TILES = listOf(CoordGrid(3926, 5250, 0), CoordGrid(3940, 5250, 0))
        private val WATERFALL_NORTH_TILES = listOf(CoordGrid(3926, 5306, 0), CoordGrid(3940, 5306, 0))
        private val PALM_TILE = CoordGrid(3934, 5278, 0)
        private val END_BARRIER_TILE = CoordGrid(3922, 5279, 0)
        private const val END_BARRIER_LENGTH = 3

        private val CONTAINER_ID: Int by lazy { CONTAINER.asRSCM(RSCMType.OBJ) }

        /** Takes every water container away from [player] (end of room, reset, leaving). */
        fun removeContainers(player: Player) {
            for (slot in player.inv.indices) {
                if (player.inv[slot]?.id == CONTAINER_ID) player.inv[slot] = null
            }
        }

        /** Slot of [player]'s water container, or `null`. */
        fun containerSlot(player: Player): Int? =
            player.inv.indices.firstOrNull { player.inv[it]?.id == CONTAINER_ID }
    }
}
