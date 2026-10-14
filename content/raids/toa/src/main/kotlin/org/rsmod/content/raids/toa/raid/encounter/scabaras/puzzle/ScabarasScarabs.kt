package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc

internal class ScabarasScarabs(private val room: ScabarasPuzzleEncounter) {
    private val deps = room.raid.deps
    private val sides = listOf(ScabarasCoords.SCARAB_SPAWNS_NORTH, ScabarasCoords.SCARAB_SPAWNS_SOUTH)
    private val scarabs = Array(sides.size) { ArrayList<Npc>() }
    private var nextCheck = 0

    fun start() {
        nextCheck = deps.mapClock.cycle + FIRST_CHECK
    }

    fun clear() {
        for (side in scarabs) {
            for (npc in side) room.despawn(npc)
            side.clear()
        }
    }

    fun tick() {
        if (room.stage != ToaStage.STARTED) return
        val now = deps.mapClock.cycle
        for (side in scarabs) side.removeIf { !it.isSlotAssigned || it.hitpoints <= 0 }
        if (now >= nextCheck) {
            nextCheck = now + CHECK_PERIOD
            topUp()
        }
    }

    private fun topUp() {
        val wanted = (room.teamSize + 1) / 2
        for ((index, side) in scarabs.withIndex()) {
            while (side.size < wanted) {
                val tile =
                    sides[index].translate(
                        deps.random.of(-SPAWN_SPREAD, SPAWN_SPREAD),
                        deps.random.of(-SPAWN_SPREAD, SPAWN_SPREAD),
                    )
                val npc = room.spawn(ScabarasNpcs.SCARAB, room.coords(tile))
                npc.anim(ScabarasSeqs.SCARAB_SPAWN)
                side += npc
            }
        }
    }

    private companion object {
        const val FIRST_CHECK = 33
        const val CHECK_PERIOD = 10
        const val SPAWN_SPREAD = 1
    }
}
