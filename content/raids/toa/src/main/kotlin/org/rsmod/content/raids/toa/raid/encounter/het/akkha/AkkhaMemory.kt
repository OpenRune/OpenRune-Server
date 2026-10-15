package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import kotlin.math.min
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class AkkhaMemory(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps

    var active = false
        private set

    fun start(boss: Npc) {
        active = true
        val feelingSpecial = room.raid.isActive("dbrow.toa_invocation_feeling_special")
        val iterations = BASE_ITERATIONS + min(MAX_EXTRA_ITERATIONS, room.pathLevel / 2)
        val gap = if (feelingSpecial) FAST_GAP else SLOW_GAP
        val firstBlast =
            LIGHT_START + LIGHT_GAP * (iterations - 1) + if (feelingSpecial) 0 else LIGHT_GAP
        val lastBlast = firstBlast + gap * (iterations - 1)
        val returnTick = lastBlast + RETURN_DELAY
        val lights = walk(iterations)

        deps.bossDeps.suppressAttacks(boss, returnTick + ATTACK_RATE)
        room.schedule(GLOW_MESSAGE) {
            for (player in room.targets()) player.mes(GLOW_TEXT)
        }
        room.schedule(DISAPPEAR_ANIM) { room.akkha?.anim(AkkhaSeqs.DISAPPEAR) }
        room.schedule(HIDDEN) { room.hideAkkha() }
        for ((i, safe) in lights.withIndex()) {
            room.schedule(LIGHT_START + LIGHT_GAP * i) { showLight(safe) }
            room.schedule(firstBlast + gap * i) {
                for (quadrant in Quadrant.entries) {
                    if (quadrant != safe) room.quadrants.memoryBlast(quadrant)
                }
            }
        }
        room.schedule(returnTick) {
            active = false
            room.showAkkha()
        }
    }

    fun clear() {
        active = false
    }

    private fun walk(count: Int): List<Quadrant> {
        val ring = Quadrant.entries
        var index = deps.random.of(0, ring.lastIndex)
        val lights = ArrayList<Quadrant>(count)
        repeat(count) {
            lights += ring[index]
            val step = if (deps.random.of(0, 1) == 0) 1 else ring.size - 1
            index = (index + step) % ring.size
        }
        return lights
    }

    private fun showLight(quadrant: Quadrant) {
        val tile = room.coords(LIGHT_TILES.getValue(quadrant))
        val existing = deps.locRepo.findAll(tile).firstOrNull()
        val angle = existing?.angle ?: LocAngle.West
        val shape = existing?.shape ?: LocShape.CentrepieceStraight
        deps.locRepo.add(tile, LIGHT_LOCS.getValue(quadrant), LIGHT_GAP, angle, shape)
    }

    private companion object {
        const val ATTACK_RATE = 6
        const val BASE_ITERATIONS = 4
        const val MAX_EXTRA_ITERATIONS = 2
        const val GLOW_MESSAGE = 2
        const val DISAPPEAR_ANIM = 3
        const val HIDDEN = 4
        const val LIGHT_START = 4
        const val LIGHT_GAP = 2
        const val SLOW_GAP = 4
        const val FAST_GAP = 2
        const val RETURN_DELAY = 2
        const val GLOW_TEXT = "Sections of the room start to glow..."

        val LIGHT_TILES =
            mapOf(
                Quadrant.NE to CoordGrid(3682, 5409, 1),
                Quadrant.SE to CoordGrid(3682, 5406, 1),
                Quadrant.SW to CoordGrid(3679, 5406, 1),
                Quadrant.NW to CoordGrid(3679, 5409, 1),
            )

        val LIGHT_LOCS =
            mapOf(
                Quadrant.NE to AkkhaLocs.GLOW_DARKNESS,
                Quadrant.SE to AkkhaLocs.GLOW_LIGHTNING,
                Quadrant.SW to AkkhaLocs.GLOW_FREEZE,
                Quadrant.NW to AkkhaLocs.GLOW_BURN,
            )
    }
}
