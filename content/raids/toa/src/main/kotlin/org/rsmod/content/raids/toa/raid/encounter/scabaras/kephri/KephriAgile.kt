package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import kotlin.math.sign
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

internal class KephriAgile(private val room: KephriEncounter) {
    private val deps = room.raid.deps
    private val scarabs = ArrayList<Scarab>()

    private class Scarab(
        val npc: Npc,
        var target: Player? = null,
        var retargetCycle: Int = 0,
        var nextAttack: Int = 0,
    )

    val count: Int
        get() = scarabs.size

    fun hatch(tile: CoordGrid) {
        val npc = room.spawn(KephriNpcs.AGILE_SCARAB, tile)
        npc.anim(KephriSeqs.SCARAB_SPAWN)
        scarabs += Scarab(npc, nextAttack = deps.mapClock.cycle + ATTACK_SPEED)
    }

    fun remove(npc: Npc) {
        scarabs.removeIf { it.npc === npc }
        room.despawn(npc)
    }

    fun clear() {
        for (scarab in scarabs) room.despawn(scarab.npc)
        scarabs.clear()
    }

    fun tick() {
        if (room.stage != ToaStage.STARTED || scarabs.isEmpty()) return
        val players = room.targets()
        val cycle = deps.mapClock.cycle
        for (scarab in scarabs.toList()) {
            val npc = scarab.npc
            if (!npc.isSlotAssigned || npc.hitpoints <= 0) {
                scarabs.remove(scarab)
                continue
            }
            if (room.dung.hasPile(npc.coords)) {
                remove(npc)
                continue
            }
            if (players.isEmpty()) continue
            if (scarab.target.let { it == null || it !in players } || cycle >= scarab.retargetCycle) {
                scarab.target = players[deps.random.of(0, players.lastIndex)]
                scarab.retargetCycle = cycle + RETARGET_TICKS
            }
            act(scarab, cycle)
        }
    }

    private fun act(scarab: Scarab, cycle: Int) {
        val npc = scarab.npc
        val target = scarab.target ?: return
        val distance = npc.coords.chebyshevDistance(target.coords)
        when {
            distance <= KITE_DISTANCE -> flee(npc, target.coords)
            distance > ATTACK_RANGE -> npc.walk(target.coords)
        }
        if (distance > ATTACK_RANGE || cycle < scarab.nextAttack) return
        scarab.nextAttack = cycle + ATTACK_SPEED
        npc.anim(KephriSeqs.SCARAB_ATTACK)
        deps.worldRepo.projAnim(
            projAnim(
                KephriSpots.SCARAB_SHOT,
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
                val damage = deps.random.of(0, MAX_HIT)
                target.queueImpactHit(npc, 1, HitType.Ranged, damage, deps.playerHitModifier)
            }
        }
    }

    private fun flee(npc: Npc, from: CoordGrid) {
        val dx = sign((npc.coords.x - from.x).toDouble()).toInt()
        val dz = sign((npc.coords.z - from.z).toDouble()).toInt()
        val away = npc.coords.translate(dx * FLEE_STEP, dz * FLEE_STEP)
        if (room.room.inChallengeArea(room.staticCoords(away)) && room.isOpenFloor(away)) {
            npc.walk(away)
        }
    }

    private companion object {
        const val ATTACK_SPEED = 4
        const val ATTACK_RANGE = 6
        const val KITE_DISTANCE = 3
        const val FLEE_STEP = 3
        const val RETARGET_TICKS = 20
        const val MAX_HIT = 5
        const val HIT_DELAY = 2
        const val START_HEIGHT = 96
        const val END_HEIGHT = 124
        const val START_TIME = 36
        const val FLIGHT = 90
        const val ANGLE = 16
        const val PROGRESS = 64
    }
}
