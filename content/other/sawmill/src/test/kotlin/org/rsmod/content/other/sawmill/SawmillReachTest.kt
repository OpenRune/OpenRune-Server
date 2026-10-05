package org.rsmod.content.other.sawmill

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.route.RayCastValidator
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SawmillReachTest {

    @Test
    fun `each operator spawns once on the tile the wiki gives`() {
        val spawns = spawns()
        assertEquals(
            mapOf(
                SawmillOperator.LumberYard to listOf(CoordGrid(3302, 3492, 0), CoordGrid(1623, 3500, 0)),
                SawmillOperator.Prifddinas to listOf(CoordGrid(3315, 6116, 0)),
                SawmillOperator.Auburnvale to listOf(CoordGrid(1395, 3369, 0)),
            ),
            SawmillOperator.entries.associateWith { op -> spawns[op.npc].orEmpty().sortedBy { it.x }.reversed() },
        )
    }

    @Test
    fun `the counter operator stays on his tile`() {
        val type = checkNotNull(ServerCacheManager.getNpc("npc.poh_sawmill_opp".asRSCM(RSCMType.NPC)))
        assertEquals(0, type.wanderRange)
    }

    @Test
    fun `every operator can be used from a walkable tile outside his room`() {
        val map = SawmillMap.build()
        val route = RouteFinding(map.collision)
        val rayCast = RayCastValidator(map.collision)

        for ((spawn, starts) in STARTS) {
            for (start in starts) {
                assertTrue(map.walkable(start), "test start $start must be walkable")
                assertTrue(
                    usable(route, rayCast, start, spawn),
                    "operator at $spawn cannot be used by a player clicking from $start",
                )
            }
        }
    }

    @Test
    fun `the Lumber Yard and Woodcutting Guild operators sit behind a counter`() {
        val map = SawmillMap.build()
        val route = RouteFinding(map.collision)
        for (spawn in listOf(CoordGrid(3302, 3492, 0), CoordGrid(1623, 3500, 0))) {
            val start = STARTS.getValue(spawn).first()
            val direct = route.findRoute(0, start.x, start.z, spawn.x, spawn.z, locShape = -2, moveNear = false)
            assertTrue(direct.failed, "$spawn is expected to need the approach trigger")
        }
    }

    @Test
    fun `line of sight is blocked by the solid walls of the room`() {
        val map = SawmillMap.build()
        val rayCast = RayCastValidator(map.collision)
        val spawn = CoordGrid(3302, 3492, 0)
        assertTrue(!rayCast.hasLineOfSight(CoordGrid(3302, 3497, 0), spawn, extraFlag = CollisionFlag.BLOCK_PLAYERS))
        assertTrue(rayCast.hasLineOfSight(CoordGrid(3302, 3491, 0), spawn, extraFlag = CollisionFlag.BLOCK_PLAYERS))
    }

    private fun usable(
        route: RouteFinding,
        rayCast: RayCastValidator,
        start: CoordGrid,
        spawn: CoordGrid,
    ): Boolean {
        val path = route.findRoute(0, start.x, start.z, spawn.x, spawn.z, locShape = -2)
        if (path.success) return true
        val end = path.waypoints.lastOrNull()?.let { CoordGrid(it.x, it.z, it.level) } ?: start
        val withinRange = maxOf(Math.abs(end.x - spawn.x), Math.abs(end.z - spawn.z)) <= APPROACH_RANGE
        return withinRange && rayCast.hasLineOfSight(end, spawn, extraFlag = CollisionFlag.BLOCK_PLAYERS)
    }

    private fun spawns(): Map<String, List<CoordGrid>> {
        val pattern =
            Regex(
                "npc = \"(npc[.][a-z0-9_]+)\"\\s*\\r?\\n" +
                    "coords = \"(\\d+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)\""
            )
        val names = SawmillOperator.entries.map { it.npc }.toSet()
        val found = mutableMapOf<String, MutableList<CoordGrid>>()
        for (file in File(".data/raw-cache/map/npcs").listFiles { f -> f.extension == "toml" }.orEmpty()) {
            for (match in pattern.findAll(file.readText())) {
                val (name, level, mx, mz, lx, lz) = match.destructured
                if (name !in names) continue
                found.getOrPut(name) { mutableListOf() } +=
                    CoordGrid(mx.toInt() * 64 + lx.toInt(), mz.toInt() * 64 + lz.toInt(), level.toInt())
            }
        }
        return found
    }

    private companion object {
        const val APPROACH_RANGE = 2

        val STARTS =
            mapOf(
                CoordGrid(3302, 3492, 0) to
                    listOf(CoordGrid(3300, 3484, 0), CoordGrid(3306, 3489, 0), CoordGrid(3302, 3490, 0)),
                CoordGrid(1623, 3500, 0) to
                    listOf(CoordGrid(1625, 3496, 0), CoordGrid(1626, 3500, 0), CoordGrid(1624, 3500, 0)),
                CoordGrid(3315, 6116, 0) to listOf(CoordGrid(3315, 6108, 0), CoordGrid(3312, 6112, 0)),
                CoordGrid(1395, 3369, 0) to listOf(CoordGrid(1395, 3360, 0), CoordGrid(1398, 3367, 0)),
            )

        @JvmStatic
        @BeforeAll
        fun cache() {
            val cache = ServerCacheManager.init(240)
            try {
                SawmillMap.load(cache)
            } finally {
                cache.close()
            }
        }
    }
}
