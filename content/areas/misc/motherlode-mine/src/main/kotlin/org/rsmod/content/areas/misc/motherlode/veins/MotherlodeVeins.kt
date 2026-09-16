package org.rsmod.content.areas.misc.motherlode.veins

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.game.MapClock
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey

/**
 * The cache map stores every vein in its depleted form, so the live veins are spawned over them.
 * A vein starts a depletion timer when its first pay-dirt is mined, reverts to the map loc when
 * that timer runs out, and is spawned again after a respawn delay.
 */
@Singleton
class MotherlodeVeins
@Inject
constructor(private val locRepo: LocRepository, private val mapClock: MapClock) {
    private val veins = HashMap<CoordGrid, Vein>()
    private var initialised = false

    fun isVein(coords: CoordGrid): Boolean = coords in veins

    fun isActive(coords: CoordGrid): Boolean = veins[coords]?.respawnCycle == NOT_DEPLETED

    fun onPayDirtMined(coords: CoordGrid) {
        val vein = veins[coords] ?: return
        if (vein.depleteCycle != NOT_STARTED || vein.respawnCycle != NOT_DEPLETED) {
            return
        }
        val lifespan = if (vein.upperLevel) UPPER_LIFESPAN else LOWER_LIFESPAN
        vein.depleteCycle = mapClock.cycle + lifespan.random()
    }

    fun tick() {
        if (!initialised) {
            spawnAll()
            initialised = true
        }
        val cycle = mapClock.cycle
        for (vein in veins.values) {
            if (vein.depleteCycle != NOT_STARTED && cycle >= vein.depleteCycle) {
                deplete(vein)
            } else if (vein.respawnCycle != NOT_DEPLETED && cycle >= vein.respawnCycle) {
                respawn(vein)
            }
        }
    }

    private fun spawnAll() {
        val southWest = MotherlodeMine.MAP_SQUARE_SOUTH_WEST
        for (zoneX in 0 until MotherlodeMine.MAP_SQUARE_LENGTH step ZoneGrid.LENGTH) {
            for (zoneZ in 0 until MotherlodeMine.MAP_SQUARE_LENGTH step ZoneGrid.LENGTH) {
                val zone = ZoneKey.from(southWest.translate(zoneX, zoneZ))
                for (loc in locRepo.findAll(zone).toList()) {
                    val type = MotherlodeVein.forDepletedId(loc.id) ?: continue
                    val oreLoc = loc.copy(entity = LocEntity(type.oreId, loc.shapeId, loc.angleId))
                    val upper = MotherlodeMine.isUpperFloor(loc.coords)
                    val vein = Vein(depleted = loc, ore = oreLoc, upperLevel = upper)
                    veins[loc.coords] = vein
                    locRepo.add(oreLoc, Int.MAX_VALUE)
                }
            }
        }
    }

    private fun deplete(vein: Vein) {
        locRepo.add(vein.depleted, Int.MAX_VALUE)
        val respawn = if (vein.upperLevel) UPPER_RESPAWN else LOWER_RESPAWN
        vein.depleteCycle = NOT_STARTED
        vein.respawnCycle = mapClock.cycle + respawn.random()
    }

    private fun respawn(vein: Vein) {
        locRepo.add(vein.ore, Int.MAX_VALUE)
        vein.respawnCycle = NOT_DEPLETED
    }

    private class Vein(val depleted: LocInfo, val ore: LocInfo, val upperLevel: Boolean) {
        var depleteCycle: Int = NOT_STARTED
        var respawnCycle: Int = NOT_DEPLETED
    }

    private fun IntRange.random(): Int = Random.nextInt(first, last + 1)

    private companion object {
        const val NOT_STARTED = -1
        const val NOT_DEPLETED = -1

        val LOWER_LIFESPAN = 38..45
        val UPPER_LIFESPAN = 60..67
        val LOWER_RESPAWN = 168..176
        val UPPER_RESPAWN = 98..101
    }
}
