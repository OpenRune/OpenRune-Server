package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * The spear statues (Offline_Scape handleSpears / handleSpearColumn). Four statue rows, each with
 * a west and an east wall of five spears on a 10-tick cycle with its own delays. A spear is
 * dangerous on 3 ticks of the cycle, over a 3-tile strip.
 */
internal class CrondisSpears(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private var cycleTick = 0

    /** Map cycle each player can be hit again (Offline_Scape temp attrs). */
    private val hitReady = HashMap<Player, Int>()

    fun tick(targets: List<Player>) {
        val active = ArrayList<CoordGrid>()
        for (row in CrondisCoords.SPEAR_ROWS.indices) {
            wall(row, east = false, active)
            wall(row, east = true, active)
        }
        for (player in targets) {
            val coords = player.coords
            if (active.any { coords.z == it.z && coords.x in it.x..it.x + 2 }) {
                room.hazardHit(player, hitReady, BASE_DAMAGE, HIT_COOLDOWN)
            }
        }
        cycleTick = (cycleTick + 1) % CYCLE
    }

    fun clear() {
        cycleTick = 0
        hitReady.clear()
    }

    /** Offline_Scape clearStatueSpikes: every trap back to idle. */
    fun idle() {
        for (row in CrondisCoords.SPEAR_ROWS) {
            val origin = room.coords(row)
            for (east in listOf(false, true)) {
                for (y in 0 until SPEARS_PER_WALL) {
                    val dz = y * 2 + if (east) 1 else 0
                    val spear = origin.translate(if (east) 4 else 2, dz)
                    val mouth = origin.translate(if (east) 7 else 0, dz)
                    animLoc(CrondisLocs.ROW_TRAP_FIRE, spear, CrondisSeqs.TRAP_IDLE)
                    animLoc(CrondisLocs.ROW_TRAP, mouth, CrondisSeqs.TRAP_IDLE)
                }
            }
        }
    }

    private fun wall(row: Int, east: Boolean, active: MutableList<CoordGrid>) {
        val origin = room.coords(CrondisCoords.SPEAR_ROWS[row])
        for (y in 0 until SPEARS_PER_WALL) {
            val delay = DELAYS[row][y + if (east) SPEARS_PER_WALL else 0]
            val dz = y * 2 + if (east) 1 else 0
            val mouth = origin.translate(if (east) 7 else 0, dz)
            val spear = origin.translate(if (east) 4 else 2, dz)
            when (cycleTick) {
                delay -> animLoc(CrondisLocs.ROW_TRAP, mouth, CrondisSeqs.TRAP_ACTIVATE)
                (delay + 3) % CYCLE ->
                    animLoc(CrondisLocs.ROW_TRAP_FIRE, spear, CrondisSeqs.TRAP_SPEAR)
                (delay + 4) % CYCLE,
                (delay + 5) % CYCLE -> active += spear
                (delay + 6) % CYCLE -> {
                    animLoc(CrondisLocs.ROW_TRAP_FIRE, spear, CrondisSeqs.TRAP_IDLE)
                    active += spear
                }
                (delay + 7) % CYCLE -> animLoc(CrondisLocs.ROW_TRAP, mouth, CrondisSeqs.TRAP_IDLE)
            }
        }
    }

    /**
     * Animates the map's own trap loc at [at]. The traps are part of the map, so they're looked
     * up rather than built; if one isn't found, only the visual is skipped (damage still works).
     */
    private fun animLoc(type: String, at: CoordGrid, seq: String) {
        val locType = ServerCacheManager.getObject(type.asRSCM(RSCMType.LOC)) ?: return
        val loc = deps.locRepo.findExact(at, locType) ?: return
        deps.worldRepo.locAnim(loc, seq)
    }

    private companion object {
        const val CYCLE = 10
        const val SPEARS_PER_WALL = 5
        const val BASE_DAMAGE = 6
        const val HIT_COOLDOWN = 3

        /** Offline_Scape STATUE_SPEAR_DELAYS: per row, 5 west spears then 5 east spears. */
        val DELAYS =
            arrayOf(
                intArrayOf(0, 2, 4, 6, 8, 0, 2, 4, 6, 8),
                intArrayOf(0, 3, 6, 9, 2, 0, 3, 6, 9, 2),
                intArrayOf(2, 1, 1, 0, 0, 2, 2, 1, 1, 0),
                intArrayOf(0, 1, 2, 3, 4, 4, 3, 2, 1, 0),
            )
    }
}
