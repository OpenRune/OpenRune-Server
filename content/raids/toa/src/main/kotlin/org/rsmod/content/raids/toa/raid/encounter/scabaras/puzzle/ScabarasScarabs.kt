package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.projAnim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

internal class ScabarasScarabs(private val room: ScabarasPuzzleEncounter) {
    private val deps = room.raid.deps
    private val sides = listOf(ScabarasCoords.SCARAB_SPAWNS_NORTH, ScabarasCoords.SCARAB_SPAWNS_SOUTH)
    private val scarabs = Array(sides.size) { ArrayList<Scarab>() }
    private var nextCheck = 0

    private class Scarab(val npc: Npc, var nextAttack: Int)

    fun start() {
        nextCheck = deps.mapClock.cycle + FIRST_CHECK
    }

    fun clear() {
        for (side in scarabs) {
            for (scarab in side) room.despawn(scarab.npc)
            side.clear()
        }
    }

    fun tick(targets: List<Player>) {
        if (room.stage != ToaStage.STARTED) return
        val now = deps.mapClock.cycle
        for (side in scarabs) side.removeIf { !it.npc.isSlotAssigned || it.npc.hitpoints <= 0 }
        if (now >= nextCheck) {
            nextCheck = now + CHECK_PERIOD
            topUp(now)
        }
        if (targets.isEmpty()) return
        for (side in scarabs) for (scarab in side) act(scarab, targets, now)
    }

    private fun topUp(now: Int) {
        val wanted = (room.teamSize + 1) / 2
        for ((index, side) in scarabs.withIndex()) {
            while (side.size < wanted) {
                val tile = sides[index].translate(
                    deps.random.of(-SPAWN_SPREAD, SPAWN_SPREAD),
                    deps.random.of(-SPAWN_SPREAD, SPAWN_SPREAD),
                )
                val npc = room.spawn(ScabarasNpcs.SCARAB, room.coords(tile))
                npc.anim(ScabarasSeqs.SCARAB_SPAWN)
                side += Scarab(npc, now + ATTACK_SPEED)
            }
        }
    }

    private fun act(scarab: Scarab, targets: List<Player>, now: Int) {
        val npc = scarab.npc
        val target = targets.minBy { it.coords.chebyshevDistance(npc.coords) }
        val distance = npc.coords.chebyshevDistance(target.coords)
        if (distance > ATTACK_RANGE) npc.walk(target.coords)
        if (distance > ATTACK_RANGE || now < scarab.nextAttack) return
        scarab.nextAttack = now + ATTACK_SPEED
        npc.anim(ScabarasSeqs.SCARAB_ATTACK)
        deps.worldRepo.projAnim(
            projAnim(
                ScabarasSpots.SCARAB_SHOT,
                npc.coords,
                target.coords,
                START_HEIGHT,
                END_HEIGHT,
                START_TIME,
                FLIGHT,
                ANGLE,
                PROGRESS,
            )
        )
        room.schedule(HIT_DELAY) {
            if (npc.isSlotAssigned && room.stage == ToaStage.STARTED) {
                val factor = room.combatantOf(npc)?.damageFactor ?: 1.0
                val damage = (deps.random.of(0, MAX_HIT) * factor).toInt()
                target.queueImpactHit(npc, 1, HitType.Ranged, damage, deps.playerHitModifier)
            }
        }
    }

    private companion object {
        const val FIRST_CHECK = 33
        const val CHECK_PERIOD = 10
        const val SPAWN_SPREAD = 1
        const val ATTACK_SPEED = 4
        const val ATTACK_RANGE = 6
        const val MAX_HIT = 6
        const val HIT_DELAY = 2
        const val START_HEIGHT = 96
        const val END_HEIGHT = 124
        const val START_TIME = 36
        const val FLIGHT = 90
        const val ANGLE = 16
        const val PROGRESS = 64
    }
}
