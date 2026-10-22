package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import kotlin.math.sign
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal class KephriSwarms(private val room: KephriEncounter) {
    private val deps = room.raid.deps
    private val swarms = ArrayList<Swarm>()
    private var anchor = 0
    private var spawning = false
    private var nextMedic = NEVER

    private class Swarm(
        val npc: Npc,
        val spawnedCycle: Int,
        val medic: Boolean = false,
        var arrived: Boolean = false,
    )

    fun begin(stun: Int, stunTicks: Int) {
        val offsets = if (stun == 1) FIRST_STUN else SECOND_STUN
        spawning = true
        anchor = deps.random.of(0, ANCHORS.lastIndex)
        val triple = tripleEvery()
        for ((index, offset) in offsets.withIndex()) {
            val triples = triple > 0 && (index + 1) % triple == 0
            room.schedule(offset) { if (spawning) spawn(triples) }
        }
        room.schedule(stunTicks - FADE_BEFORE_WAKE) { fade() }
    }

    fun startMedic() {
        nextMedic =
            if (room.raid.isActive("dbrow.toa_invocation_medic")) deps.mapClock.cycle + MEDIC_INTERVAL else NEVER
    }

    fun tick() {
        if (room.stage != ToaStage.STARTED) return
        medicTick()
        val boss = room.kephri ?: return
        val centre = boss.kephriCentre
        for (swarm in swarms.toList()) {
            val npc = swarm.npc
            if (!npc.isSlotAssigned || npc.hitpoints <= 0) {
                swarms.remove(swarm)
                continue
            }
            if (swarm.arrived || deps.mapClock.cycle - swarm.spawnedCycle < UNTARGETABLE_TICKS) {
                continue
            }
            npc.ignoreCombatInteractions = false
            if (npc.coords.chebyshevDistance(centre) <= ARRIVAL_DISTANCE) {
                arrive(swarm, boss)
            } else {
                step(npc, centre)
            }
        }
    }

    fun remove(npc: Npc) {
        swarms.removeIf { it.npc === npc }
        room.despawn(npc)
    }

    private fun medicTick() {
        val cycle = deps.mapClock.cycle
        if (nextMedic == NEVER || cycle < nextMedic) return
        nextMedic += MEDIC_INTERVAL
        if (!room.isStunned) spawn(false, medic = true)
    }

    fun clear() {
        nextMedic = NEVER
        spawning = false
        for (swarm in swarms) room.despawn(swarm.npc)
        swarms.clear()
    }

    fun spawnMedic() = spawn(false, medic = true)

    private fun spawn(triple: Boolean, medic: Boolean = false) {
        if (!medic && !room.isStunned) return
        val span = ANCHORS[anchor]
        anchor = (anchor + deps.random.of(1, 2)) % ANCHORS.size
        val tiles =
            if (triple) span else listOf(span[deps.random.of(0, span.lastIndex)])
        for (tile in tiles) {
            val npc = room.spawn(KephriNpcs.SHIELD_SCARAB, room.coords(tile))
            npc.ignoreCombatInteractions = true
            npc.anim(KephriSeqs.SWARM_SPAWN)
            swarms += Swarm(npc, deps.mapClock.cycle, medic)
        }
    }

    private fun step(npc: Npc, centre: CoordGrid) {
        var next = npc.coords
        repeat(STEPS_PER_TICK) {
            if (next.chebyshevDistance(centre) <= ARRIVAL_DISTANCE) return@repeat
            next = next.translate(sign((centre.x - next.x).toDouble()).toInt(), sign((centre.z - next.z).toDouble()).toInt())
        }
        npc.teleport(deps.collision, next)
    }

    private fun arrive(swarm: Swarm, boss: Npc) {
        swarm.arrived = true
        swarm.npc.anim(KephriSeqs.SWARM_INTEGRATE)
        deps.worldRepo.soundArea(swarm.npc.coords, KephriSynths.SWARM_ABSORB, radius = ABSORB_SOUND_RADIUS)
        val range = if (swarm.medic) MEDIC_RANGE else SHIELD_RANGE
        room.healKephri(boss.baseHitpointsLvl * deps.random.of(range.first, range.last) / PER_MILLE)
        room.schedule(REMOVE_DELAY) { remove(swarm.npc) }
    }

    private fun fade() {
        spawning = false
        for (swarm in swarms.toList()) {
            if (swarm.arrived || swarm.medic) continue
            swarm.arrived = true
            swarm.npc.anim(KephriSeqs.SWARM_DEATH)
            room.schedule(REMOVE_DELAY) { remove(swarm.npc) }
        }
    }

    private fun tripleEvery(): Int =
        when (room.pathLevel) {
            in 0..1 -> 0
            in 2..3 -> 8
            in 4..5 -> 6
            else -> 4
        }

    private companion object {
        val FIRST_STUN =
            listOf(5, 9, 13, 17, 20, 23, 26, 29, 31, 33, 35, 37) + (38..53).toList()
        val SECOND_STUN =
            listOf(6, 10, 14, 18, 21, 24, 27, 30, 32, 34, 36, 38) + (39..50).toList()

        const val FADE_BEFORE_WAKE = 3
        const val ABSORB_SOUND_RADIUS = 8
        const val UNTARGETABLE_TICKS = 4
        const val STEPS_PER_TICK = 2
        const val ARRIVAL_DISTANCE = 3
        const val REMOVE_DELAY = 2
        const val NEVER = Int.MIN_VALUE
        const val MEDIC_INTERVAL = 12
        val SHIELD_RANGE = 95..105
        val MEDIC_RANGE = 180..200
        const val PER_MILLE = 1000

        val ANCHORS =
            listOf(
                span(3559, 3559, 5411, 5413),
                span(3559, 3559, 5403, 5405),
                span(3551, 3553, 5400, 5400),
                span(3543, 3543, 5403, 5405),
                span(3543, 3543, 5411, 5413),
                span(3551, 3553, 5416, 5416),
            )

        fun span(minX: Int, maxX: Int, minZ: Int, maxZ: Int): List<CoordGrid> =
            (minX..maxX).flatMap { x -> (minZ..maxZ).map { z -> CoordGrid(x, z, 0) } }
    }
}
