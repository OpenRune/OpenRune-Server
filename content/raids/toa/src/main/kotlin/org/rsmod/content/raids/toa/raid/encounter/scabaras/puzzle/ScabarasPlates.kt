package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class LocSet(private val room: ScabarasPuzzleEncounter) {
    private val locs = HashMap<Pair<CoordGrid, LocShape>, LocInfo>()

    fun set(
        static: CoordGrid,
        loc: String,
        shape: LocShape = LocShape.GroundDecor,
        angle: LocAngle = LocAngle.West,
    ) {
        locs[static to shape] = room.addLoc(static, loc, angle, shape)
    }

    fun remove(static: CoordGrid, shape: LocShape = LocShape.GroundDecor) {
        locs.remove(static to shape)?.let(room::delLoc)
    }

    fun clear() {
        for (loc in locs.values) room.delLoc(loc)
        locs.clear()
    }
}

internal class ScabarasMemory(private val room: ScabarasPuzzleEncounter) {
    private val locs = LocSet(room)
    private val deps = room.raid.deps
    private var slot = -1
    private var plates = emptyList<CoordGrid>()
    private var sequence: List<Int>? = null
    private var stepIndex = 0
    private var inputFrom = 0

    private val done: Boolean
        get() = slot >= 0 && room.completed[slot]

    fun build(slot: Int) {
        this.slot = slot
        sequence = null
        stepIndex = 0
        plates = ScabarasCoords.MEMORY_PLATES.map { room.plate(slot, it) }
        locs.set(
            room.plate(slot, ScabarasCoords.BUTTON_OFFSET),
            ScabarasLocs.MEMORY_BUTTON,
            LocShape.CentrepieceStraight,
        )
        for (tile in plates) locs.set(tile, ScabarasLocs.MEMORY_UP)
    }

    fun clear() {
        locs.clear()
        sequence = null
    }

    fun press(player: Player) {
        if (!room.canUse(slot)) return
        if (done) return player.mes(ScabarasMessages.ALREADY_DONE)
        val now = room.raid.deps.mapClock.cycle
        if (sequence != null && now < inputFrom) return player.mes(ScabarasMessages.WAIT_SEQUENCE)
        player.anim(ScabarasSeqs.PRESS_BUTTON)
        player.soundSynth(ScabarasSynths.BUTTON, delay = BUTTON_SOUND_DELAY)
        val chosen = deps.random.shuffled(plates.indices.toList()).take(SEQUENCE_LENGTH)
        sequence = chosen
        stepIndex = 0
        for ((order, index) in chosen.withIndex()) {
            room.schedule(FIRST_FLASH + order) { flash(plates[index]) }
        }
        inputFrom = now + FIRST_FLASH + SEQUENCE_LENGTH
    }

    fun step(player: Player, static: CoordGrid) {
        val current = sequence ?: return
        val index = plates.indexOf(static)
        if (index < 0 || done || !room.canUse(slot)) return
        if (deps.mapClock.cycle < inputFrom) return
        if (index != current[stepIndex]) {
            sequence = null
            return room.smallRocks(player)
        }
        player.soundSynth(ScabarasSynths.STEP)
        locs.set(static, ScabarasLocs.MEMORY_PLAYER)
        room.schedule(MARK_TICKS) { if (!done) locs.set(static, ScabarasLocs.MEMORY_UP) }
        stepIndex++
        if (stepIndex < current.size) return
        sequence = null
        for (tile in plates) locs.set(tile, ScabarasLocs.MEMORY_DOWN)
        room.completeSlot(slot)
    }

    private fun flash(tile: CoordGrid) {
        if (done) return
        locs.set(tile, ScabarasLocs.MEMORY_DOWN)
        room.schedule(1) { if (!done) locs.set(tile, ScabarasLocs.MEMORY_UP) }
    }

    private companion object {
        const val SEQUENCE_LENGTH = 5
        const val FIRST_FLASH = 2
        const val MARK_TICKS = 2
        const val BUTTON_SOUND_DELAY = 10
    }
}

internal class ScabarasSum(private val room: ScabarasPuzzleEncounter) {
    private val locs = LocSet(room)
    private val deps = room.raid.deps
    private var slot = -1
    private var plates = emptyMap<CoordGrid, Int>()
    private var goal = 0
    private var total = 0

    private val done: Boolean
        get() = slot >= 0 && room.completed[slot]

    fun build(slot: Int) {
        this.slot = slot
        goal = 0
        total = 0
        plates =
            buildMap {
                for ((value, offsets) in ScabarasCoords.SUM_PLATES) {
                    for (offset in offsets) put(room.plate(slot, offset), value)
                }
            }
        locs.set(
            room.plate(slot, ScabarasCoords.BUTTON_OFFSET),
            ScabarasLocs.SUM_TABLET,
            LocShape.CentrepieceStraight,
        )
        for ((tile, value) in plates) locs.set(tile, ScabarasLocs.sumTile(value))
    }

    fun clear() {
        locs.clear()
    }

    fun read(player: Player) {
        if (!room.canUse(slot)) return
        if (done) return player.mes(ScabarasMessages.ALREADY_DONE)
        if (goal == 0) goal = deps.random.of(GOAL_MIN, GOAL_MAX)
        player.mes(ScabarasMessages.sumGoal(goal))
    }

    fun step(player: Player, static: CoordGrid) {
        val value = plates[static] ?: return
        if (goal == 0 || done || !room.canUse(slot)) return
        player.soundSynth(ScabarasSynths.STEP)
        total += value
        glow(static, value)
        when {
            total == goal -> {
                for ((tile, v) in plates) glow(tile, v)
                room.completeSlot(slot)
            }
            total > goal -> {
                total = 0
                for (tile in plates.keys) locs.remove(tile, LocShape.CentrepieceStraight)
                room.smallRocks(player)
            }
        }
    }

    fun debugSetGoal(value: Int) {
        goal = value
    }

    private fun glow(tile: CoordGrid, value: Int) {
        locs.set(tile, ScabarasLocs.glow(SUM_GLOW[value]), LocShape.CentrepieceStraight)
    }

    private companion object {
        const val GOAL_MIN = 20
        const val GOAL_MAX = 45
    }
}

internal class ScabarasLights(private val room: ScabarasPuzzleEncounter) {
    private val locs = LocSet(room)
    private val deps = room.raid.deps
    private var slot = -1
    private var ring = emptyList<CoordGrid>()
    private val lit = BooleanArray(RING_SIZE)

    private val done: Boolean
        get() = slot >= 0 && room.completed[slot]

    fun build(slot: Int) {
        this.slot = slot
        ring = ScabarasCoords.LIGHT_RING.map { room.plate(slot, it) }
        do {
            for (i in lit.indices) lit[i] = deps.random.randomBoolean()
        } while (lit.all { it })
        for (tile in ring) locs.set(tile, ScabarasLocs.LIGHT_OFF)
        for (i in lit.indices) render(i)
    }

    fun clear() {
        locs.clear()
    }

    fun step(player: Player, static: CoordGrid) {
        val index = ring.indexOf(static)
        if (index < 0 || done || !room.canUse(slot)) return
        player.soundSynth(ScabarasSynths.STEP)
        toggle(index - 1)
        toggle(index)
        toggle(index + 1)
        finish()
    }

    fun flip(player: Player, static: CoordGrid) {
        val index = ring.indexOf(static)
        if (index < 0 || done || !room.canUse(slot)) return
        player.anim(ScabarasSeqs.FLIP_START)
        player.soundSynth(ScabarasSynths.FLIP)
        room.schedule(FLIP_TICKS) {
            toggle(index)
            player.anim(ScabarasSeqs.FLIP_END)
            room.hazardDamage(player, FLIP_DAMAGE, 0)
            finish()
        }
    }

    private fun toggle(index: Int) {
        val wrapped = Math.floorMod(index, RING_SIZE)
        lit[wrapped] = !lit[wrapped]
        render(wrapped)
    }

    private fun finish() {
        if (lit.all { it }) room.completeSlot(slot)
    }

    private fun render(index: Int) {
        val tile = ring[index]
        if (lit[index]) {
            locs.set(tile, ScabarasLocs.glow(1), LocShape.CentrepieceStraight)
        } else {
            locs.remove(tile, LocShape.CentrepieceStraight)
        }
    }

    private companion object {
        const val RING_SIZE = 8
        const val FLIP_TICKS = 4
        const val FLIP_DAMAGE = 20
    }
}
