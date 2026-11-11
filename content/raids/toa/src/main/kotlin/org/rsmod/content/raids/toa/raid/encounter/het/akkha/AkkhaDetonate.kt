package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.map.CoordGrid

internal class AkkhaDetonate(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps

    fun start() {
        val targets = room.targets()
        if (targets.size < 2) return
        val count = (targets.size + 1) / 2
        val marked = deps.random.shuffled(targets).take(count)
        room.schedule(MARK_DELAY) {
            for (player in marked) {
                player.mes(MARKED_TEXT)
                player.tint(MARK_TINT)
            }
        }
        room.schedule(BLAST_DELAY) { detonate(marked) }
    }

    private fun detonate(marked: List<Player>) {
        val diagonal =
            room.raid.isActive("dbrow.toa_invocation_feeling_special") && deps.random.of(0, 1) == 0
        val directions = if (diagonal) DIAGONALS else CARDINALS
        val bystanders = room.targets()
        for (source in marked) {
            val centre = source.coords
            val quadrant = room.quadrants.quadrantOf(source) ?: continue
            for ((dx, dz) in directions) {
                for (step in 1..ARM_LENGTH) {
                    val tile = centre.translate(dx * step, dz * step)
                    if (!floor(tile)) break
                    deps.worldRepo.spotanimMap(
                        spotanim(quadrant.gfx),
                        tile,
                        height = quadrant.gfxHeight,
                    )
                    for (player in bystanders) {
                        if (player.coords != tile) continue
                        room.hazardDamage(player, DAMAGE, SPREAD)
                        room.effects.apply(quadrant, player)
                    }
                }
            }
        }
    }

    private fun floor(tile: CoordGrid): Boolean {
        val static = room.staticCoords(tile)
        return static != CoordGrid.NULL && room.room.inChallengeArea(static) && room.isOpenFloor(tile)
    }

    private companion object {
        const val MARK_DELAY = 2
        const val BLAST_DELAY = 6
        const val ARM_LENGTH = 3
        const val DAMAGE = 15
        const val SPREAD = 2
        const val MARKED_TEXT = "You have been marked for detonation!"
        val MARK_TINT =
            EntityTinting(
                startCycle = 0,
                endCycle = 125,
                hue = 8,
                saturation = 1,
                lightness = 100,
                weight = 112,
            )
        val CARDINALS = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        val DIAGONALS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
    }
}
