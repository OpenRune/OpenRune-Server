package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

/**
 * The acid trails (Offline_Scape spawnAcidTrails / AcidTrail). From each basin, 2-3 of its 5
 * columns start a trail: an acid orb appears for 3 ticks, then 2 ticks later the trail runs 10
 * tiles across the room.
 */
internal class CrondisAcid(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private val splash = SpotanimType(CrondisSpots.ACID.asRSCM(RSCMType.SPOTANIM))
    private val trails = ArrayList<Trail>()
    private var countdown = 0

    /** Map cycle each player can be hit again (Offline_Scape temp attrs). */
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
            val columns = shuffledColumns()
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

    /** 0..4 in random order. Hand-rolled because randomness must go through GameRandom. */
    private fun shuffledColumns(): IntArray {
        val columns = IntArray(COLUMNS) { it }
        for (i in columns.lastIndex downTo 1) {
            val j = deps.random.of(maxExclusive = i + 1)
            val swap = columns[i]
            columns[i] = columns[j]
            columns[j] = swap
        }
        return columns
    }

    private fun start(trail: Trail) {
        trails += trail
        for (step in 1..TRAIL_LENGTH) {
            val tile = trail.base.translate(0, if (trail.moveNorth) step else -step)
            // Delay in client cycles: the splash travels along the trail.
            deps.worldRepo.spotanimMap(splash, tile, delay = SPLASH_CYCLES * step)
        }
    }

    private inner class Trail(val base: CoordGrid, val moveNorth: Boolean) {
        private var ticks = TRAIL_TICKS

        /** Counts down; `true` once the trail is spent. */
        fun expire(): Boolean = --ticks <= 0

        /**
         * Hits a player in the trail's column while the splash passes their tile: the further
         * from the basin, the later (Offline_Scape AcidTrail.check).
         */
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
        const val MIN_TRAILS = 2
        const val MAX_TRAILS = 3
        const val ORB_TICKS = 3
        const val TRAIL_DELAY = 2
        const val TRAIL_TICKS = 6
        const val TRAIL_LENGTH = 10
        const val SPLASH_CYCLES = 10
        const val BASE_DAMAGE = 5
        const val HIT_COOLDOWN = 2
    }
}
