package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.map.CoordGrid

internal class AkkhaTrail(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps
    private val orbs = ArrayList<Orb>()
    private val lastTile = HashMap<Player, CoordGrid>()
    private var running = false
    private var feelingSpecial = false

    private class Orb(val npc: Npc, val tile: CoordGrid, val quadrant: Quadrant, val expires: Int)

    fun start() {
        if (running) return
        running = true
        feelingSpecial = room.raid.isActive("dbrow.toa_invocation_feeling_special")
        lastTile.clear()
        room.schedule(START_MESSAGE) {
            val text = if (feelingSpecial) AROUND_TEXT else BEHIND_TEXT
            for (player in room.targets()) player.mes(text)
        }
        loop(1)
    }

    fun clear() {
        running = false
        lastTile.clear()
        for (orb in orbs) room.despawn(orb.npc)
        orbs.clear()
    }

    private fun loop(tick: Int) {
        room.schedule(1) {
            step(tick)
            if (tick < STOP_MESSAGE || orbs.isNotEmpty()) {
                loop(tick + 1)
            } else {
                running = false
            }
        }
    }

    private fun step(tick: Int) {
        if (room.stage != ToaStage.STARTED) return
        val cycle = deps.mapClock.cycle
        val targets = room.targets()
        expire(cycle)
        for (player in targets) {
            val now = player.coords
            val before = lastTile.put(player, now)
            if (before != null && before != now && tick in FIRST_SPAWN..LAST_SPAWN) {
                spawnOrb(before, cycle)
                if (feelingSpecial) spawnOrb(now.translate(now.x - before.x, now.z - before.z), cycle)
            }
        }
        tintTargets(tick, targets)
        if (tick == STOP_MESSAGE) for (player in targets) player.mes(STOP_TEXT)
        trigger(targets, cycle)
    }

    private fun tintTargets(tick: Int, targets: List<Player>) {
        val tint =
            when {
                tick == START_MESSAGE -> START_TINT
                tick in PULSE_FIRST..PULSE_LAST -> pulse(tick)
                tick == STOP_MESSAGE -> CLEAR_TINT
                else -> return
            }
        for (player in targets) player.tint(tint)
    }

    private fun spawnOrb(tile: CoordGrid, cycle: Int) {
        val static = room.staticCoords(tile)
        if (static == CoordGrid.NULL || !room.room.inChallengeArea(static)) return
        if (!room.isOpenFloor(tile) || orbs.any { it.tile == tile }) return
        val quadrant = Quadrant.of(static)
        val npc = room.spawn(quadrant.trailOrb, tile)
        deps.worldRepo.soundArea(tile, AkkhaSynths.ORB, radius = ORB_SOUND_RADIUS)
        orbs += Orb(npc, tile, quadrant, cycle + ORB_LIFETIME)
    }

    private fun expire(cycle: Int) {
        val iterator = orbs.iterator()
        while (iterator.hasNext()) {
            val orb = iterator.next()
            if (orb.expires > cycle && room.stage == ToaStage.STARTED) continue
            room.despawn(orb.npc)
            iterator.remove()
        }
    }

    private fun trigger(targets: List<Player>, cycle: Int) {
        val iterator = orbs.iterator()
        while (iterator.hasNext()) {
            val orb = iterator.next()
            val victim = targets.firstOrNull { it.coords == orb.tile } ?: continue
            deps.worldRepo.spotanimMap(
                spotanim(orb.quadrant.gfx),
                orb.tile,
                height = orb.quadrant.gfxHeight,
            )
            room.hazardDamage(victim, ORB_DAMAGE, ORB_SPREAD)
            room.effects.apply(orb.quadrant, victim)
            room.despawn(orb.npc)
            iterator.remove()
        }
    }

    private companion object {
        const val START_MESSAGE = 2
        const val ORB_SOUND_RADIUS = 4
        const val FIRST_SPAWN = 3
        const val LAST_SPAWN = 20
        const val STOP_MESSAGE = 22
        const val PULSE_FIRST = 3
        const val PULSE_LAST = 21
        const val PULSE_CYCLES = 60
        const val PULSE_LIGHTNESS = 15

        val START_TINT =
            EntityTinting(
                startCycle = 0,
                endCycle = 605,
                hue = 0,
                saturation = 0,
                lightness = 0,
                weight = 112,
            )
        val CLEAR_TINT =
            EntityTinting(
                startCycle = 0,
                endCycle = 0,
                hue = -1,
                saturation = -1,
                lightness = -1,
                weight = 0,
            )

        fun pulse(tick: Int) =
            EntityTinting(
                startCycle = 0,
                endCycle = PULSE_CYCLES,
                hue = 0,
                saturation = 0,
                lightness = if (tick % 2 == 1) PULSE_LIGHTNESS else 0,
                weight = 112,
            )
        const val ORB_LIFETIME = 7
        const val ORB_DAMAGE = 13
        const val ORB_SPREAD = 2
        const val BEHIND_TEXT = "Magical orbs begin to materialise behind you!"
        const val AROUND_TEXT = "Magical orbs begin to materialise around you!"
        const val STOP_TEXT = "The magical orbs stop materialising."
    }
}
