package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Npc
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class KephriEggs(private val room: KephriEncounter) {
    private val deps = room.raid.deps
    private val eggs = ArrayList<Egg>()
    private var generation = 0

    private class Egg(val npc: Npc, val dark: Boolean, val tile: CoordGrid)

    fun begin(boss: Npc) {
        val centre = boss.coords.translate(KephriCoords.CENTRE_OFFSET, KephriCoords.CENTRE_OFFSET)
        val tiles =
            deps.random.shuffled(
                EGG_TILES.map(room::coords).filter { room.isOpenFloor(it) && !room.dung.hasPile(it) }
            )
        val launched = generation
        val dark = darkCount().coerceAtMost(tiles.size)
        for ((index, tile) in tiles.withIndex()) {
            val isDark = index < dark
            deps.worldRepo.projAnim(
                ProjAnim(
                    spotanim = (if (isDark) KephriSpots.EGG_DARK else KephriSpots.EGG_SMALL).asRSCM(RSCMType.SPOTANIM),
                    startHeight = START_HEIGHT,
                    endHeight = 0,
                    startTime = START_TIME,
                    endTime = FLIGHT,
                    angle = ANGLE,
                    progress = PROGRESS,
                    sourceIndex = 0,
                    targetIndex = 0,
                    startCoord = centre,
                    endCoord = tile,
                )
            )
            room.schedule(LAND_DELAY) { if (launched == generation) land(tile, isDark, launched) }
        }
    }

    fun killed(npc: Npc) {
        val egg = eggs.firstOrNull { it.npc === npc } ?: return
        eggs.remove(egg)
        deps.worldRepo.spotanimMap(spotanim(KephriSpots.EGG_BREAK), egg.tile)
        room.despawn(npc)
    }

    fun clear() {
        generation++
        for (egg in eggs) room.despawn(egg.npc)
        eggs.clear()
    }

    private fun land(tile: CoordGrid, dark: Boolean, launched: Int) {
        if (room.stage != ToaStage.STARTED) return
        val npc = room.spawn(if (dark) KephriNpcs.EGG_HATCH else KephriNpcs.EGG_EXPLODE, tile)
        npc.anim(KephriSeqs.EGG_SPAWN)
        val egg = Egg(npc, dark, tile)
        eggs += egg
        for (player in room.targets()) {
            if (player.coords == tile) player.hitTypeless(room.maxHit(PLACED_DAMAGE).coerceAtLeast(1))
        }
        room.schedule(DETONATE_DELAY) { if (launched == generation) detonate(egg) }
    }

    private fun detonate(egg: Egg) {
        if (egg !in eggs || room.stage != ToaStage.STARTED) return
        eggs.remove(egg)
        room.despawn(egg.npc)
        if (egg.dark) {
            deps.worldRepo.spotanimMap(spotanim(KephriSpots.EGG_BREAK), egg.tile)
            room.agile.hatch(egg.tile)
            return
        }
        for (dx in -1..1) {
            for (dz in -1..1) {
                val explosion = KephriSpots.EXPLOSIONS[deps.random.of(0, KephriSpots.EXPLOSIONS.lastIndex)]
                deps.worldRepo.spotanimMap(spotanim(explosion), egg.tile.translate(dx, dz), height = IMPACT_HEIGHT)
            }
        }
        for (player in room.targets()) {
            if (player.coords.chebyshevDistance(egg.tile) > 1) continue
            player.hitTypeless(deps.random.of(0, room.maxHit(EXPLOSION_DAMAGE)))
        }
    }

    private fun darkCount(): Int {
        val players = room.teamSize
        if (!room.raid.isActive(ToaInvocationKey.LivelyLarvae)) return DARK_PER_PLAYER * players
        return deps.random.of(LIVELY_MIN * players, LIVELY_MAX * players)
    }

    private companion object {
        const val DARK_PER_PLAYER = 2
        const val LIVELY_MIN = 3
        const val LIVELY_MAX = 5
        const val LAND_DELAY = 4
        const val DETONATE_DELAY = 15
        const val FLIGHT = 120
        const val START_TIME = 70
        const val START_HEIGHT = 344
        const val ANGLE = 14
        const val PROGRESS = 96
        const val PLACED_DAMAGE = 2
        const val EXPLOSION_DAMAGE = 24
        const val IMPACT_HEIGHT = 38

        val EGG_TILES =
            listOf(
                CoordGrid(3551, 5404, 0),
                CoordGrid(3549, 5402, 0),
                CoordGrid(3545, 5406, 0),
                CoordGrid(3551, 5412, 0),
                CoordGrid(3549, 5414, 0),
                CoordGrid(3545, 5410, 0),
                CoordGrid(3547, 5408, 0),
                CoordGrid(3553, 5402, 0),
                CoordGrid(3557, 5406, 0),
                CoordGrid(3553, 5414, 0),
                CoordGrid(3555, 5408, 0),
                CoordGrid(3557, 5410, 0),
            )
    }
}
