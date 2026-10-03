package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class CrondisAcid(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private val splash = SpotanimType(CrondisSpots.ACID.asRSCM(RSCMType.SPOTANIM))
    private val trails = ArrayList<Trail>()
    private var countdown = 0

    private val hitReady = HashMap<Player, Int>()

    fun tick(targets: List<Player>) {
        trails.removeIf { it.expire() }
        for (player in targets) {
            for (trail in trails) trail.check(player)
        }
        if (--countdown <= 0) {
            countdown = INTERVAL
            spawn(CrondisCoords.ACID_NORTH_BASES, moveNorth = false)
            spawn(CrondisCoords.ACID_SOUTH_BASES, moveNorth = true)
        }
    }

    fun clear() {
        trails.clear()
        countdown = 0
        hitReady.clear()
    }

    private fun spawn(bases: List<CoordGrid>, moveNorth: Boolean) {
        for (base in bases) {
            val middle = room.coords(base.translate(ACID_COLUMNS_MIDDLE, 0))
            deps.worldRepo.soundArea(middle, CrondisSynths.ACID_ORB, radius = ORB_SOUND_RADIUS)
            val columns = deps.random.shuffled((0 until COLUMNS).toList())
            val count = deps.random.of(MIN_TRAILS, MAX_TRAILS)
            for (i in 0 until count) {
                val tile = room.coords(base.translate(columns[i], 0))
                val shape = LocShape.CentrepieceStraight
                deps.locRepo.add(tile, CrondisLocs.ACID_ORB, ORB_TICKS, LocAngle.West, shape)
                val trail = Trail(tile, moveNorth)
                room.schedule(TRAIL_DELAY) { start(trail) }
            }
        }
    }

    private fun start(trail: Trail) {
        trails += trail
        for (step in 1..TRAIL_LENGTH) {
            val tile = trail.base.translate(0, if (trail.moveNorth) step else -step)
            deps.worldRepo.spotanimMap(splash, tile, delay = SPLASH_CYCLES * step)
        }
    }

    private inner class Trail(val base: CoordGrid, val moveNorth: Boolean) {
        private var ticks = TRAIL_TICKS

        fun expire(): Boolean = --ticks <= 0

        fun check(player: Player) {
            val coords = player.coords
            if (coords.x != base.x) return
            val dz = coords.z - base.z
            val ahead = if (moveNorth) dz else -dz
            if (ahead !in 1..TRAIL_LENGTH) return
            val sub = 1 + (abs(dz) - 1) / 3
            val window = TRAIL_TICKS - sub
            if (ticks < window && ticks >= window - 2) {
                room.hazardHit(player, hitReady, BASE_DAMAGE, HIT_COOLDOWN)
            }
        }
    }

    private companion object {
        const val INTERVAL = 5
        const val COLUMNS = 5
        const val ACID_COLUMNS_MIDDLE = 2
        const val ORB_SOUND_RADIUS = 14
        const val MIN_TRAILS = 2
        const val MAX_TRAILS = 3
        const val ORB_TICKS = 3
        const val TRAIL_DELAY = 2
        const val TRAIL_TICKS = 6
        const val TRAIL_LENGTH = 10
        const val SPLASH_CYCLES = 10
        const val BASE_DAMAGE = 4
        const val HIT_COOLDOWN = 2
    }
}
