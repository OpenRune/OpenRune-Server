package org.rsmod.content.bosses.moons.neypotzli

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Bream fishing in the Streambound Cavern. Fish swim down both rivers all the time, whether or not
 * anyone is there. Every other tick one sets off from a random source towards a random lane at the
 * bend, and three ticks later it turns for the lane it ends in at the fishing spot. That last lane
 * is the same in both rivers and is settled as the fish sets off: the tick after, everyone whose
 * net is turned to it nets the fish, well before it is seen to arrive.
 */
class RiverFishing
@Inject
constructor(
    private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>,
    private val worldRepo: WorldRepository,
    private val worldQueues: WorldQueueList,
    private val random: GameRandom,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val sessions = IdentityHashMap<Player, Session>()
    private var cycle = 0
    private var releasedLane = NO_LANE

    override fun ScriptContext.startup() {
        onOpLoc1("loc.river_fishing_spot") { castOrAlign(it.loc.coords) }
        onOpLoc1("loc.river_fishing_spot_enter") { castOrAlign(it.loc.coords) }
        onApLoc1("loc.river_fishing_clickzone_active") { alignNet(it.loc.coords) }
        onPlayerLogin {
            if (player.vars["varbit.river_fishing_is_fishing"] != 0) {
                player.set("varbit.river_fishing_is_fishing", 0)
            }
        }
        onPlayerLogout { sessions -= player }
        worldQueues.add(1, ::tick)
    }

    private suspend fun ProtectedAccess.castOrAlign(clicked: CoordGrid) {
        if (sessions.containsKey(player)) {
            alignNet(clicked)
            return
        }
        val river = RIVERS.firstOrNull { it.isSpot(clicked) } ?: return
        if (!neypotzliOpen(hooks)) return
        if (statBase("stat.fishing") < FISHING_LEVEL) {
            mes("You need a Fishing level of $FISHING_LEVEL to fish here.")
            return
        }
        if (inv.count("obj.big_net") == 0) {
            mes("You need a big fishing net to fish here.")
            return
        }
        if (inv.freeSpace() == 0) {
            mes("Your inventory is too full to hold any more fish.")
            return
        }
        sessions[player] = Session(river, coords, cycle)
        player.set("varbit.river_fishing_dir", MIDDLE_LANE)
        player.set("varbit.river_fishing_is_fishing", 1)
        faceSquare(river.mouth[MIDDLE_LANE])
        anim("seq.human_largenet_looping")
    }

    private fun ProtectedAccess.alignNet(clicked: CoordGrid) {
        val session = sessions[player] ?: return
        val lane = session.river.laneOf(clicked) ?: return
        session.lastClick = cycle
        faceSquare(session.river.mouth[lane])
        if (player.vars["varbit.river_fishing_dir"] == lane) return
        player.set("varbit.river_fishing_dir", lane)
        anim("seq.human_largenet_looping")
    }

    private fun stop(player: Player) {
        sessions -= player
        player.set("varbit.river_fishing_is_fishing", 0)
        player.resetAnim()
    }

    private fun tick() {
        cycle++
        for ((player, session) in sessions.entries.toList()) {
            val idle = cycle - session.lastClick >= IDLE_TICKS
            if (player.coords != session.stand || idle) stop(player)
        }
        if (cycle % RELEASE_INTERVAL == 0) {
            release()
        } else {
            turnAtBend()
            net()
        }
        worldQueues.add(1, ::tick)
    }

    private fun release() {
        val mouth = randomLane()
        releasedLane = mouth
        for (river in RIVERS) {
            val bend = randomLane()
            river.upstream.addLast(Fish(bend, mouth))
            val source = river.sources[random.of(0, river.sources.lastIndex)]
            swim(source, river.bend[bend], UPPER_LEG)
        }
    }

    private fun turnAtBend() {
        for (river in RIVERS) {
            if (river.upstream.size <= 1) continue
            val fish = river.upstream.removeFirst()
            swim(river.bend[fish.bend], river.mouth[fish.mouth], LOWER_LEG)
        }
    }

    private fun net() {
        if (releasedLane == NO_LANE) return
        for (player in sessions.keys.toList()) {
            player.anim("seq.human_largenet_looping")
            if (player.vars["varbit.river_fishing_dir"] == releasedLane) land(player)
        }
    }

    private fun land(player: Player) {
        protectedAccess.launch(player) {
            val space = inv.freeSpace()
            if (space == 0) {
                stop(player)
                mes("Your inventory is too full to hold any more fish.")
                return@launch
            }
            val extra = space > 1 && statRandom("stat.fishing", EXTRA_LOW, EXTRA_HIGH, 0)
            invAdd(inv, "obj.bream_fish_raw", if (extra) 2 else 1)
            soundSynth(CATCH_SYNTHS[random.of(0, CATCH_SYNTHS.lastIndex)])
            if (extra) {
                spam("You manage to catch an additional fish!")
                val level = statBase("stat.fishing").coerceIn(FISHING_LEVEL, MAX_LEVEL)
                val scale = (level - FISHING_LEVEL).toDouble() / (MAX_LEVEL - FISHING_LEVEL)
                statAdvance("stat.fishing", EXTRA_XP_MIN + (EXTRA_XP_MAX - EXTRA_XP_MIN) * scale)
            } else {
                statAdvance("stat.fishing", BREAM_XP)
            }
            if (inv.freeSpace() == 0) {
                stop(player)
                mes("Your inventory is too full to hold any more fish.")
            }
        }
    }

    private fun swim(from: CoordGrid, to: CoordGrid, clientCycles: Int) {
        worldRepo.projAnim(
            ProjAnim(
                spotanim = "spotanim.pmoon_fish".asRSCM(RSCMType.SPOTANIM),
                startHeight = 0,
                endHeight = 0,
                startTime = 0,
                endTime = clientCycles,
                angle = FISH_ANGLE,
                progress = 0,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private fun randomLane(): Int = random.of(0, LANES - 1)

    private class Fish(val bend: Int, val mouth: Int)

    private class Session(val river: River, val stand: CoordGrid, started: Int) {
        var lastClick = started
    }

    /** [bend] and [mouth] hold one tile per lane, in the order `river_fishing_dir` counts them. */
    private class River(
        val spot: CoordGrid,
        val sources: List<CoordGrid>,
        val bend: List<CoordGrid>,
        val mouth: List<CoordGrid>,
        val netZone: List<CoordGrid>,
        val laneAlongX: Boolean,
    ) {
        val upstream = ArrayDeque<Fish>()

        fun isSpot(coords: CoordGrid): Boolean = coords.x == spot.x && coords.z == spot.z

        fun laneOf(coords: CoordGrid): Int? {
            if (netZone.none { it.x == coords.x && it.z == coords.z }) return null
            return if (laneAlongX) coords.x - mouth[0].x else coords.z - mouth[0].z
        }
    }

    private companion object {
        private const val LANES = 3
        private const val MIDDLE_LANE = 1
        private const val NO_LANE = -1
        private const val FISHING_LEVEL = 20
        private const val MAX_LEVEL = 99
        private const val RELEASE_INTERVAL = 2
        private const val UPPER_LEG = 90
        private const val LOWER_LEG = 80
        private const val FISH_ANGLE = 255
        private const val IDLE_TICKS = 50
        private const val BREAM_XP = 3.0
        private const val EXTRA_XP_MIN = 7.0
        private const val EXTRA_XP_MAX = 35.0

        // UNVERIFIED: the odds of a second bream below level 99, where it has only been seen to
        // succeed.
        private const val EXTRA_LOW = 16
        private const val EXTRA_HIGH = 256

        private val CATCH_SYNTHS =
            listOf(
                "synth.varlamore_pm_fish_catch_01",
                "synth.varlamore_pm_fish_catch_02",
                "synth.varlamore_pm_fish_catch_03",
            )

        private val RIVERS =
            listOf(
                River(
                    spot = CoordGrid(1501, 9698),
                    sources = (1504..1506).map { CoordGrid(it, 9689) },
                    bend = (1501..1503).map { CoordGrid(it, 9693) },
                    mouth = (1500..1502).map { CoordGrid(it, 9698) },
                    netZone = (1500..1502).flatMap { x -> (9694..9698).map { CoordGrid(x, it) } },
                    laneAlongX = true,
                ),
                River(
                    spot = CoordGrid(1521, 9689),
                    sources = (9688..9690).map { CoordGrid(1528, it) },
                    bend = (9688..9690).map { CoordGrid(1524, it) },
                    mouth = (9688..9690).map { CoordGrid(1521, it) },
                    netZone = (1521..1526).flatMap { x -> (9688..9690).map { CoordGrid(x, it) } },
                    laneAlongX = false,
                ),
            )
    }
}
