package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction

internal class HetOrbs(private val room: HetPuzzleEncounter) {
    private val deps = room.raid.deps
    private val orbs = ArrayList<Orb>()
    private var waveDir = 0

    private class Orb(val npc: Npc, val dir: HetDir)

    fun clear() {
        for (orb in orbs) room.despawn(orb.npc)
        orbs.clear()
        waveDir = 0
    }

    fun tick(elapsed: Int, targets: List<Player>) {
        move(targets)
        if (elapsed >= FIRST_WAVE && (elapsed - FIRST_WAVE) % WAVE_PERIOD == 0) wave()
    }

    private fun move(targets: List<Player>) {
        val iterator = orbs.iterator()
        while (iterator.hasNext()) {
            val orb = iterator.next()
            val next = orb.npc.coords.translate(orb.dir.dx, orb.dir.dz)
            if (room.orbBlocked(next)) {
                room.orbHitsMirror(next)
                pop(orb.npc)
                iterator.remove()
                continue
            }
            orb.npc.teleport(deps.collision, next)
            val victim = targets.firstOrNull { it.coords == next }
            if (victim != null) {
                room.hazardDamage(victim, ORB_DAMAGE, ORB_SPREAD)
                pop(orb.npc)
                iterator.remove()
            }
        }
    }

    private fun wave() {
        val dir = WAVE_ORDER[waveDir++ % WAVE_ORDER.size]
        repeat(ORBS_PER_WAVE) { spawn(dir) }
    }

    private fun spawn(dir: HetDir) {
        repeat(SPAWN_ATTEMPTS) {
            val tile = room.orbSpawnTile(dir, deps.random) ?: return@repeat
            val npc = room.spawn(HetNpcs.ORB, tile, Direction.South)
            npc.anim(ORB_WALK)
            deps.worldRepo.spotanimMap(spotanim(HetSpots.ORB_POP), tile)
            orbs += Orb(npc, dir)
            return
        }
    }

    private fun pop(npc: Npc) {
        deps.worldRepo.spotanimMap(spotanim(HetSpots.ORB_POP), npc.coords)
        deps.worldRepo.soundArea(npc.coords, HetSynths.ORB_POP, radius = POP_SOUND_RADIUS)
        room.despawn(npc)
    }

    private companion object {
        const val FIRST_WAVE = 11
        const val WAVE_PERIOD = 7
        const val ORBS_PER_WAVE = 2
        const val SPAWN_ATTEMPTS = 12
        const val ORB_DAMAGE = 15
        const val ORB_SPREAD = 4
        const val POP_SOUND_RADIUS = 4
        const val ORB_WALK = "seq.toa_het_orb_walk01"

        val WAVE_ORDER = listOf(HetDir.East, HetDir.South, HetDir.West, HetDir.North)
    }
}
