package org.rsmod.content.skills.mining.scripts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Singleton
class CalcifiedMine
@Inject
constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val worldQueues: WorldQueueList,
    private val playerList: PlayerList,
    private val random: GameRandom,
    private val areaChecker: AreaChecker,
) {
    private val emptyByRock: Map<Int, String> =
        ROCKS.associate { (rock, empty) -> rock.asRSCM(RSCMType.LOC) to empty }

    private val armed = HashMap<CoordGrid, Int>()
    private val wet = HashMap<CoordGrid, LocInfo>()
    private var watering = false
    private var generation = 0

    fun start() {
        scheduleOoze()
    }

    fun isWet(coords: CoordGrid): Boolean = watering && coords in wet

    fun arm(rock: BoundLocInfo) {
        if (armed.containsKey(rock.coords)) return
        val token = ++generation
        armed[rock.coords] = token
        worldQueues.add(ROCK_LIFETIME) {
            if (armed[rock.coords] == token) {
                armed.remove(rock.coords)
                exhaust(rock.coords)
            }
        }
    }

    private fun scheduleOoze() {
        worldQueues.add(random.of(OOZE_GAP.first, OOZE_GAP.last)) { startWater() }
    }

    private fun startWater() {
        scheduleOoze()
        if (watering) return
        val miners = playersInMine()
        if (miners.isEmpty()) return
        for (rock in pickWetRocks(miners)) {
            val decor =
                locRepo.add(
                    rock.coords,
                    WATER_LOC,
                    Int.MAX_VALUE,
                    rock.angle,
                    LocShape.WallDecorStraightNoOffset,
                )
            wet[rock.coords] = decor
        }
        watering = true
        for (player in miners) {
            player.mes(OOZE_MESSAGE)
            player.soundSynth(OOZE_SOUND)
        }
        worldQueues.add(random.of(WATER_TICKS.first, WATER_TICKS.last)) { stopWater() }
    }

    private fun stopWater() {
        watering = false
        for (decor in wet.values) locRepo.del(decor, Int.MAX_VALUE)
        worldQueues.add(1) { depleteWetRocks() }
    }

    private fun depleteWetRocks() {
        val coords = wet.keys.toList()
        wet.clear()
        for (at in coords) {
            armed.remove(at)
            exhaust(at)
        }
    }

    private fun exhaust(coords: CoordGrid) {
        val rock = locRepo.findAll(coords).firstOrNull { it.id in emptyByRock } ?: return
        val empty = emptyByRock.getValue(rock.id)
        locRepo.add(coords, empty, ROCK_RESPAWN, rock.angle, rock.shape)
        worldRepo.soundArea(coords, DEPLETE_SOUND, delay = DEPLETE_SOUND_DELAY, radius = 1)
    }

    private fun pickWetRocks(miners: List<Player>): List<LocInfo> {
        val standing = standingRocks(miners)
        val picked = mutableListOf<LocInfo>()
        for (east in listOf(false, true)) {
            val half =
                standing
                    .filter { areaChecker.inArea("area.cam_torum_mine_east", it.coords) == east }
                    .toMutableList()
            repeat(WET_PER_HALF) {
                if (half.isEmpty()) return@repeat
                picked += half.removeAt(random.of(0, half.lastIndex))
            }
        }
        return picked
    }

    private fun standingRocks(miners: List<Player>): List<LocInfo> {
        val zones = HashSet<ZoneKey>()
        for (miner in miners) {
            val centre = ZoneKey.from(miner.coords)
            for (dx in -SCAN_ZONES..SCAN_ZONES) {
                for (dz in -SCAN_ZONES..SCAN_ZONES) {
                    zones += centre.translate(dx, dz)
                }
            }
        }
        val rocks = mutableListOf<LocInfo>()
        for (zone in zones) {
            for (loc in locRepo.findAll(zone)) {
                if (loc.id in emptyByRock && inMine(loc.coords)) rocks += loc
            }
        }
        return rocks
    }

    private fun playersInMine(): List<Player> = playerList.filter { inMine(it.coords) }

    private fun inMine(coords: CoordGrid): Boolean = areaChecker.inArea("area.cam_torum_mine", coords)

    companion object {
        const val WET_BOOST_PERCENT = 115
        private const val ROCK_LIFETIME = 117
        private const val ROCK_RESPAWN = 50
        private const val WET_PER_HALF = 2
        private const val SCAN_ZONES = 5
        private val OOZE_GAP = 117..146
        private val WATER_TICKS = 38..54
        private const val WATER_LOC = "loc.varlamore_mining_special_rock"
        private const val OOZE_MESSAGE = "<col=a53fff>Some cracks around the cave begin to ooze water."
        private const val OOZE_SOUND = "synth.cave_bubbling_loop_1"
        private const val DEPLETE_SOUND = "synth.prospect"
        private const val DEPLETE_SOUND_DELAY = 10

        private val ROCKS =
            listOf(
                "loc.varlamore_mining_rock" to "loc.varlamore_mining_rock_empty",
                "loc.varlamore_mining_rock02" to "loc.varlamore_mining_rock_empty02",
                "loc.varlamore_mining_rock03" to "loc.varlamore_mining_rock_empty03",
                "loc.varlamore_mining_rock04" to "loc.varlamore_mining_rock_empty04",
            )
    }
}
