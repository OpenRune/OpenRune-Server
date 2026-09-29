package org.rsmod.content.bosses.barrows

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

@Singleton
class BarrowsDoors @Inject constructor(private val locRepo: LocRepository) {
    private val doorIds: Set<Int> by lazy {
        (BarrowsDoorway.entries.flatMap { it.locs } +
                listOf("loc.barrows_door_unlocked_r", "loc.barrows_door_unlocked_l"))
            .map { it.asRSCM(RSCMType.LOC) }
            .toSet()
    }

    fun swingOpen(clicked: BoundLocInfo) {
        val leaves = listOfNotNull(locRepo.findExact(clicked.coords, clicked.shape), partner(clicked))
        for (leaf in leaves) {
            val right = ServerCacheManager.getObject(leaf.id)?.internalName?.endsWith("_r") == true
            val swungType = if (right) "loc.barrows_door_inactive_r" else "loc.barrows_door_inactive_l"
            val swungAngle = leaf.angle.turn(if (right) 1 else 3)
            locRepo.del(leaf, OPEN_TICKS)
            locRepo.add(leaf.coords, "loc.inviswall", OPEN_TICKS, leaf.angle, leaf.shape)
            locRepo.add(leaf.coords.acrossEdge(leaf.angle), swungType, OPEN_TICKS, swungAngle, leaf.shape)
        }
    }

    private fun partner(clicked: BoundLocInfo): LocInfo? {
        val horizontal = clicked.angle == LocAngle.North || clicked.angle == LocAngle.South
        val candidates =
            if (horizontal) listOf(clicked.coords.translate(1, 0), clicked.coords.translate(-1, 0))
            else listOf(clicked.coords.translate(0, 1), clicked.coords.translate(0, -1))
        return candidates
            .mapNotNull { locRepo.findExact(it, clicked.shape) }
            .firstOrNull { it.id in doorIds && it.angle == clicked.angle }
    }

    private fun CoordGrid.acrossEdge(angle: LocAngle): CoordGrid =
        when (angle) {
            LocAngle.West -> translate(-1, 0)
            LocAngle.North -> translate(0, 1)
            LocAngle.East -> translate(1, 0)
            LocAngle.South -> translate(0, -1)
        }

    private companion object {
        const val OPEN_TICKS = 3
    }
}
