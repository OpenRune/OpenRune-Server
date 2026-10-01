package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class CrondisSpears(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private var elapsed = 0

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
        elapsed++
    }

    fun clear() {
        elapsed = 0
        hitReady.clear()
    }

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
            val first = FIRST[row][y + if (east) SPEARS_PER_WALL else 0]
            val dz = y * 2 + if (east) 1 else 0
            val mouth = origin.translate(if (east) 7 else 0, dz)
            val spear = origin.translate(if (east) 4 else 2, dz)
            if (elapsed < first) {
                if (elapsed >= first - PRE_IDLE) {
                    animLoc(CrondisLocs.ROW_TRAP, mouth, CrondisSeqs.TRAP_IDLE)
                    animLoc(CrondisLocs.ROW_TRAP_FIRE, spear, CrondisSeqs.TRAP_IDLE)
                }
                continue
            }
            val phase = (elapsed - first) % CYCLE
            if (phase == 0) animLoc(CrondisLocs.ROW_TRAP, mouth, CrondisSeqs.TRAP_ACTIVATE)
            if (phase >= MOUTH_IDLE) animLoc(CrondisLocs.ROW_TRAP, mouth, CrondisSeqs.TRAP_IDLE)
            if (phase == SPEAR_OUT) {
                animLoc(CrondisLocs.ROW_TRAP_FIRE, spear, CrondisSeqs.TRAP_SPEAR)
                val sound = CrondisSynths.SPEAR_OUT
                deps.worldRepo.soundArea(spear, sound, delay = SOUND_DELAY, radius = SOUND_RADIUS)
            }
            if (phase >= SPEAR_IDLE) {
                animLoc(CrondisLocs.ROW_TRAP_FIRE, spear, CrondisSeqs.TRAP_IDLE)
            }
            if (phase in DANGER) active += spear
        }
    }

    private fun animLoc(type: String, at: CoordGrid, seq: String) {
        val locType = ServerCacheManager.getObject(type.asRSCM(RSCMType.LOC)) ?: return
        val loc = deps.locRepo.findExact(at, locType) ?: return
        deps.worldRepo.locAnim(loc, seq)
    }

    private companion object {
        const val CYCLE = 10
        const val SPEAR_OUT = 3
        const val SPEAR_IDLE = 6
        const val MOUTH_IDLE = 7
        const val PRE_IDLE = 2
        val DANGER = 4..6
        const val SPEARS_PER_WALL = 5
        const val BASE_DAMAGE = 4
        const val HIT_COOLDOWN = 3
        const val SOUND_DELAY = 14
        const val SOUND_RADIUS = 5

        val FIRST =
            arrayOf(
                intArrayOf(2, 4, 6, 8, 10, 2, 4, 6, 8, 10),
                intArrayOf(4, 7, 10, 13, 16, 4, 7, 10, 13, 16),
                intArrayOf(4, 3, 3, 2, 2, 4, 4, 3, 3, 2),
                intArrayOf(3, 4, 5, 6, 7, 7, 6, 5, 4, 3),
            )
    }
}
