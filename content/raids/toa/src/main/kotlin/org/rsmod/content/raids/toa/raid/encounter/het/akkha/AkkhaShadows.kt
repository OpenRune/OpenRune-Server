package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.content.raids.toa.raid.encounter.HpThresholds
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal class AkkhaShadows(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps
    private val slots = Quadrant.entries.associateWith { Slot() }
    private val dispelled = HashSet<Quadrant>()
    private val thresholds = HpThresholds(THRESHOLDS, catchUp = false)

    private class Slot {
        var shadow: Npc? = null
        var watch: Npc? = null
        var alive = false
        var immune = false
    }

    var spawned = false
        private set

    private var counter = 0
    private var hold = 0
    private var lastDisrupt = -1
    private var tintedBoss: Npc? = null

    fun aliveAt(quadrant: Quadrant): Boolean = slots.getValue(quadrant).alive

    fun shielded(boss: Npc): Boolean {
        if (!spawned) return false
        val centre = room.staticCoords(boss.coords.translate(1, 1))
        if (centre == CoordGrid.NULL) return false
        return aliveAt(Quadrant.of(centre))
    }

    fun isImmune(npc: Npc): Boolean = slots.values.any { it.shadow === npc && it.immune }

    fun onAkkhaHit(boss: Npc) {
        if (!thresholds.crossed(boss)) return
        room.schedule(1) {
            if (!spawned) {
                spawned = true
                room.messageAll(APPEAR_TEXT)
                populate(initial = true)
            } else {
                room.messageAll(RESTORE_TEXT)
                populate(initial = false)
            }
        }
    }

    fun disrupt() {
        if (!spawned || hold > 0) return
        val cycle = room.raid.deps.mapClock.cycle
        if (cycle == lastDisrupt) return
        lastDisrupt = cycle
        counter = maxOf(0, counter - 1)
    }

    fun tick() {
        syncBossTint()
        if (!spawned) return
        if (hold > 0) {
            if (--hold == 0) {
                counter = 0
                refreshLocs()
            }
            showFill(FULL)
            return
        }
        if (++counter >= SLAM_COUNTER) {
            slam()
            return
        }
        showFill(counter * FILL_PER_TICK)
    }

    fun dispel(npc: Npc) {
        val entry = slots.entries.firstOrNull { it.value.shadow === npc && it.value.alive } ?: return
        val slot = entry.value
        slot.alive = false
        dispelled += entry.key
        npc.anim(AkkhaSeqs.DEATH)
        room.schedule(DEATH_TICKS) { room.despawn(npc) }
        slot.watch?.removeHeadbar(stopwatchId)
        for (other in slots.values) {
            if (!other.alive) continue
            other.immune = true
            other.shadow?.removeHeadbar(healthId)
            other.shadow?.tint(IMMUNE_TINT)
        }
        refreshLocs()
    }

    fun finalStand() {
        spawned = false
        for ((quadrant, slot) in slots) {
            val npc = slot.shadow
            if (slot.alive && npc != null) {
                npc.anim(AkkhaSeqs.DEATH)
                room.schedule(DEATH_TICKS) { room.despawn(npc) }
            }
            slot.watch?.removeHeadbar(stopwatchId)
            slot.alive = false
            setLoc(quadrant, WATCH_LOCS.getValue(quadrant) + LocState.EMPTY_DEAD.suffix)
        }
    }

    fun clear() {
        tintedBoss = null
        spawned = false
        counter = 0
        hold = 0
        dispelled.clear()
        thresholds.reset()
        for (slot in slots.values) wipe(slot)
        for (quadrant in Quadrant.entries) restoreLoc(quadrant)
    }

    private fun slam() {
        hold = HOLD_TICKS
        for ((quadrant, slot) in slots) {
            if (!slot.alive) continue
            slot.shadow?.anim(AkkhaSeqs.SHADOW_SLAM)
            room.schedule(1) { if (slot.alive) room.quadrants.shadowSlam(quadrant) }
        }
        refreshLocs()
        showFill(FULL)
    }

    private fun populate(initial: Boolean) {
        counter = 0
        hold = 0
        for ((quadrant, slot) in slots) {
            wipe(slot)
            slot.immune = quadrant in dispelled
            slot.alive = true
            val shadow = spawnClone(room, AkkhaNpcs.SHADOW, quadrant)
            slot.shadow = shadow
            slot.watch = room.spawn(AkkhaNpcs.HEADBAR, room.coords(WATCH_TILES.getValue(quadrant)))
            if (initial || !slot.immune) shadow.anim(AkkhaSeqs.SHADOW_SPAWN)
            if (slot.immune) {
                shadow.removeHeadbar(healthId)
                shadow.tint(IMMUNE_TINT)
            }
        }
        refreshLocs()
        showFill(0)
    }

    private fun syncBossTint() {
        val boss = room.akkha ?: return
        val shielded = shielded(boss)
        if (shielded == (tintedBoss === boss)) return
        tintedBoss = if (shielded) boss else null
        boss.tint(if (shielded) IMMUNE_TINT else CLEAR_TINT)
    }

    private fun wipe(slot: Slot) {
        slot.shadow?.let(room::despawn)
        slot.watch?.let(room::despawn)
        slot.shadow = null
        slot.watch = null
        slot.alive = false
        slot.immune = false
    }

    private fun showFill(fill: Int) {
        val bar = Headbar.fromNoSource(stopwatchId, stopwatchId, fill, fill, 0, 0)
        for (slot in slots.values) if (slot.alive) slot.watch?.showHeadbar(bar)
    }

    private fun refreshLocs() {
        for ((quadrant, slot) in slots) {
            val state =
                when {
                    !slot.alive -> LocState.EMPTY_DEAD
                    hold > 0 -> LocState.FULL
                    else -> LocState.EMPTY_ALIVE
                }
            setLoc(quadrant, WATCH_LOCS.getValue(quadrant) + state.suffix)
        }
    }

    private fun restoreLoc(quadrant: Quadrant) = setLoc(quadrant, WATCH_LOCS.getValue(quadrant))

    private fun setLoc(quadrant: Quadrant, type: String) {
        val tile = room.coords(WATCH_TILES.getValue(quadrant))
        val existing = deps.locRepo.findAll(tile).firstOrNull()
        val angle = existing?.angle ?: LocAngle.West
        val shape = existing?.shape ?: LocShape.CentrepieceStraight
        deps.locRepo.add(tile, type, Int.MAX_VALUE, angle, shape)
    }

    private enum class LocState(val suffix: String) {
        EMPTY_ALIVE("_empty02"),
        EMPTY_DEAD("_empty01"),
        FULL("_full01"),
    }

    private val stopwatchId: Int
        get() = AkkhaHeadbars.STOPWATCH.asRSCM(RSCMType.HEADBAR)

    private val healthId: Int
        get() = AkkhaHeadbars.SHADOW_HEALTH.asRSCM(RSCMType.HEADBAR)

    internal companion object {
        fun cornerTile(quadrant: Quadrant): CoordGrid = SHADOW_TILES.getValue(quadrant)

        fun spawnClone(room: AkkhaEncounter, type: String, quadrant: Quadrant): Npc {
            val tile = room.coords(cornerTile(quadrant))
            val clone = room.spawn(type, tile, CORNER_FACING.getValue(quadrant))
            clone.ignoreCombatInteractions = true
            faceCorner(clone, quadrant)
            return clone
        }

        fun faceCorner(npc: Npc, quadrant: Quadrant) {
            val facing = CORNER_FACING.getValue(quadrant)
            val centre = (npc.size - 1) / 2
            npc.lockFacing(npc.coords.translate(centre + facing.xOff * 8, centre + facing.zOff * 8))
        }

        private val IMMUNE_TINT =
            EntityTinting(
                startCycle = 0,
                endCycle = 600,
                hue = 0,
                saturation = 0,
                lightness = 106,
                weight = 40,
            )

        private val CLEAR_TINT =
            EntityTinting(
                startCycle = 0,
                endCycle = 0,
                hue = 255,
                saturation = 255,
                lightness = 255,
                weight = 0,
            )

        private val CORNER_FACING =
            mapOf(
                Quadrant.NE to Direction.NorthEast,
                Quadrant.SE to Direction.SouthEast,
                Quadrant.SW to Direction.SouthWest,
                Quadrant.NW to Direction.NorthWest,
            )

        private val THRESHOLDS = doubleArrayOf(0.8, 0.6, 0.4, 0.2)
        private const val SLAM_COUNTER = 50
        private const val FILL_PER_TICK = 2
        private const val FULL = 100
        private const val HOLD_TICKS = 4
        private const val DEATH_TICKS = 2
        private const val APPEAR_TEXT = "Shadows appear throughout the room!"
        private const val RESTORE_TEXT = "The shadows in the room are restored!"

        private val SHADOW_TILES =
            mapOf(
                Quadrant.NE to CoordGrid(3685, 5412, 1),
                Quadrant.SE to CoordGrid(3685, 5401, 1),
                Quadrant.SW to CoordGrid(3674, 5401, 1),
                Quadrant.NW to CoordGrid(3674, 5412, 1),
            )

        private val WATCH_TILES =
            mapOf(
                Quadrant.NE to CoordGrid(3689, 5416, 1),
                Quadrant.SE to CoordGrid(3689, 5398, 1),
                Quadrant.SW to CoordGrid(3671, 5398, 1),
                Quadrant.NW to CoordGrid(3671, 5416, 1),
            )

        private val WATCH_LOCS =
            mapOf(
                Quadrant.NE to "loc.akkha_arena_outer_lvl2_23",
                Quadrant.SE to "loc.akkha_arena_outer_lvl2_58",
                Quadrant.SW to "loc.akkha_arena_outer_lvl2_57",
                Quadrant.NW to "loc.akkha_arena_outer_lvl2_22",
            )
    }
}
