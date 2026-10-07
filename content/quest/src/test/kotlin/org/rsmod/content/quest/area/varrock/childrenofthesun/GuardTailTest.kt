package org.rsmod.content.quest.area.varrock.childrenofthesun

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.types.MoveRestrict
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.routefinder.LineValidator
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class GuardTailTest {
    private val route = CotsRoute.Tiles
    private val lookouts = CotsRoute.LookoutIndices.sorted()
    private val open: (CoordGrid, CoordGrid) -> Boolean = { _, _ -> true }
    private val blind: (CoordGrid, CoordGrid) -> Boolean = { _, _ -> false }

    private fun tail(canSee: (CoordGrid, CoordGrid) -> Boolean) =
        GuardTail(route, CotsRoute.LookoutIndices, canSee)

    private fun isBlocked(x: Int, z: Int, level: Int = 0): Boolean =
        collision[x, z, level] and
            (CollisionFlag.LOC or CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR) != 0

    private fun varrockSight(): (CoordGrid, CoordGrid) -> Boolean {
        val validator = LineValidator(collision)
        return { from, to -> validator.hasLineOfSight(from.level, from.x, from.z, to.x, to.z) }
    }

    private fun simulate(
        canSee: (CoordGrid, CoordGrid) -> Boolean,
        playerAt: (GuardTail) -> CoordGrid,
    ): Pair<TailResult, Int> {
        val tail = tail(canSee)
        repeat(1000) { tick ->
            val result = tail.tick(playerAt(tail))
            if (result != TailResult.Continue) return result to tick
        }
        error("Tail never finished")
    }

    private fun hiddenPlan(tail: GuardTail): CoordGrid {
        val passed = lookouts.count { it < tail.index }
        return HideSpots[passed.coerceAtMost(HideSpots.lastIndex)]
    }

    @Test fun `the route is made of single steps from the start to the building`() {
        assertEquals(CoordGrid(3225, 3427), route.first())
        assertEquals(CoordGrid(3258, 3400), route.last())
        for ((from, to) in route.zipWithNext()) {
            assertEquals(1, maxOf(abs(to.x - from.x), abs(to.z - from.z)))
        }
        assertEquals(CotsRoute.Lookouts.size, lookouts.size)
        assertTrue(lookouts.all { it in 1 until route.lastIndex })
    }

    @Test fun `the guard walks one tile per tick and stops at every lookout`() {
        val tail = tail(blind)
        val seen = mutableListOf<Int>()
        var ticks = 0
        while (true) {
            val result = tail.tick(tail.guard)
            ticks++
            if (tail.index in lookouts && seen.lastOrNull() != tail.index) seen += tail.index
            if (result == TailResult.Arrived) break
            assertEquals(TailResult.Continue, result)
        }
        assertEquals(lookouts, seen)
        val pause = GuardTail.StopTicks + GuardTail.TurnTicks + GuardTail.LookTicks - 1
        assertEquals(GuardTail.SetupTicks + route.lastIndex + lookouts.size * pause + 1, ticks)
    }

    @Test fun `the guard only spots the player once his turn has played out`() {
        val tail = tail(open)
        var tick = 0
        var turnedOn = -1
        var result: TailResult
        do {
            result = tail.tick(tail.guard.translateX(-2))
            if (tail.startedTurning) turnedOn = tick
            tick++
        } while (result == TailResult.Continue)
        assertEquals(TailResult.Spotted, result)
        assertTrue(turnedOn >= 0)
        assertEquals(GuardTail.TurnTicks, tick - 1 - turnedOn)
    }

    @Test fun `a guard held up on the way neither moves on nor looks until he gets there`() {
        val tail = tail(open)
        val stuck = route.first()
        repeat(50) {
            assertEquals(TailResult.Continue, tail.tick(stuck.translateX(-2), at = stuck))
        }
        assertEquals(1, tail.index)
        assertFalse(tail.isLooking)
    }

    @Test fun `sight is judged from where the guard really stands`() {
        val froms = mutableListOf<CoordGrid>()
        val tail = GuardTail(route, CotsRoute.LookoutIndices) { from, _ ->
            froms += from
            false
        }
        while (!tail.startedTurning) tail.tick(tail.guard.translateX(-2))
        val pushed = tail.guard.translateZ(-1)
        repeat(GuardTail.TurnTicks + 1) { tail.tick(tail.guard.translateX(-2), pushed) }
        assertTrue(tail.isLooking)
        assertTrue(froms.isNotEmpty())
        assertTrue(froms.all { it == pushed }, "$froms")
    }

    @Test fun `anyone with line of sight is spotted when the guard turns around`() {
        val tail = tail(open)
        var result: TailResult
        do {
            result = tail.tick(tail.guard.translateX(-2))
        } while (result == TailResult.Continue)
        assertEquals(TailResult.Spotted, result)
        assertEquals(lookouts.first(), tail.index)
        assertTrue(tail.isLooking)
    }

    @Test fun `a player who cannot be seen follows the guard to the building`() {
        val (result, _) = simulate(blind) { it.guard.translateX(-2) }
        assertEquals(TailResult.Arrived, result)
    }

    @Test fun `falling too far behind fails the tail`() {
        val start = route.first()
        val (result, _) = simulate(blind) { start }
        assertEquals(TailResult.TooFar, result)
    }

    @Test fun `changing floors fails the tail`() {
        val tail = tail(blind)
        assertEquals(TailResult.TooFar, tail.tick(CoordGrid(route.first().x, route.first().z, 1)))
    }

    @Test fun `being too far away is not the same as being seen`() {
        val tail = tail(open)
        val far = route.first().translateX(GuardTail.MaxDistance + 1)
        assertEquals(TailResult.TooFar, tail.tick(far))
    }

    @Test fun `the real route is walkable tile by tile`() {
        val steps = StepValidator(collision)
        for ((from, to) in route.zipWithNext()) {
            assertFalse(isBlocked(to.x, to.z), "$to is blocked")
            assertTrue(
                steps.canTravel(0, from.x, from.z, to.x - from.x, to.z - from.z),
                "cannot step $from -> $to",
            )
        }
    }

    @Test fun `each safe spot hides the player from its lookout`() {
        val sight = varrockSight()
        for ((index, lookout) in CotsRoute.Lookouts.withIndex()) {
            val spot = HideSpots[index]
            assertFalse(sight(lookout, spot), "$spot is visible from $lookout")
        }
    }

    @Test fun `standing in the open at a lookout gets the player spotted`() {
        val sight = varrockSight()
        for ((index, lookout) in CotsRoute.Lookouts.withIndex()) {
            val spot = ExposedSpots[index]
            assertTrue(sight(lookout, spot), "$spot is hidden from $lookout")
        }
    }

    @Test fun `hiding at every lookout gets the player to the building unseen`() {
        val (result, _) = simulate(varrockSight(), ::hiddenPlan)
        assertEquals(TailResult.Arrived, result)
    }

    @Test fun `skipping a hiding place gets the player spotted at that lookout`() {
        for (skipped in CotsRoute.Lookouts.indices) {
            val plan = { tail: GuardTail ->
                val passed = lookouts.count { it < tail.index }
                if (passed == skipped) ExposedSpots[skipped] else hiddenPlan(tail)
            }
            val (result, _) = simulate(varrockSight(), plan)
            assertEquals(TailResult.Spotted, result, "lookout ${skipped + 1}")
        }
    }

    @Test fun `the guard ends beside the door of the bandits' building`() {
        val last = route.last()
        assertEquals(1, abs(last.x - CotsRoute.Door.x))
        assertEquals(0, last.z - CotsRoute.Door.z)
    }

    @Test fun `every tile the quest places an actor on is walkable`() {
        val places = mutableMapOf<String, CoordGrid>()
        with(CotsPlaces) {
            places["king"] = King
            places["advisor"] = Advisor
            Guards.forEachIndexed { i, c -> places["guard$i"] = c }
            DelegationStart.forEachIndexed { i, c -> places["delegateStart$i"] = c }
            DelegationStop.forEachIndexed { i, c -> places["delegateStop$i"] = c }
            KnightsStart.forEachIndexed { i, c -> places["knightStart$i"] = c }
            KnightsStop.forEachIndexed { i, c -> places["knightStop$i"] = c }
            places["tobynScene"] = TobynScene
            places["bagGuardStart"] = BagGuardStart
            places["bagGuardStop"] = BagGuardStop
            places["bagGuardInside"] = BagGuardInside
            places["eavesdropVantage"] = EavesdropVantage
            places["roofArrival"] = RoofArrival
            places["roofTobyn"] = RoofTobyn
            places["roofItzla"] = RoofItzla
            places["roofCell"] = RoofCell
        }
        for ((name, tile) in places) {
            assertFalse(isBlocked(tile.x, tile.z, tile.level), "$name $tile is blocked")
        }
    }

    @Test fun `every static spawn is on a walkable tile and only one file spawns the npcs`() {
        val dir = Path.of(".data", "raw-cache", "map", "npcs")
        val spawn =
            Regex("""npc = "npc\.(vmq1_[a-z0-9_]+)"\s+coords = "(\d)_(\d+)_(\d+)_(\d+)_(\d+)"""")
        val ours = dir.resolve("varrock_children_of_the_sun.toml")
        val spawned = mutableListOf<String>()
        Files.list(dir).use { files ->
            for (file in files) {
                val text = Files.readString(file)
                if (file != ours) {
                    assertFalse(text.contains("vmq1_"), "$file spawns quest npcs")
                    continue
                }
                for (match in spawn.findAll(text)) {
                    val (name, level, mx, mz, lx, lz) = match.destructured
                    val x = mx.toInt() * 64 + lx.toInt()
                    val z = mz.toInt() * 64 + lz.toInt()
                    assertFalse(isBlocked(x, z, level.toInt()), "$name at $x,$z is blocked")
                    spawned += name
                }
            }
        }
        val expected =
            listOf("alina", "noah", "guard_sergeant", "guard_sergeant_roof", "itzla") +
                listOf("knight_1_castle", "knight_2_castle", "bag_guard_varrock") +
                (1..4).map { "bandit_${it}_varrock" } +
                listOf("bandit_4_cell") +
                (1..10).map { "guard_$it" }
        assertEquals(expected.map { "vmq1_$it" }.sorted(), spawned.sorted())
    }

    private class Runtime {
        val npcs = NpcList()
        val tails =
            GuardTails(
                NpcRepository(MapClock(100), NpcRegistry(npcs, collision, EventBus()), npcs),
                RayCastValidator(collision),
            )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                uuid = 801L
                slotId = 1
                coords = CotsRoute.Tiles.first()
                currentMapClock = 100
                processedMapClock = 100
            }

        val access =
            ProtectedAccess(
                player,
                GameCoroutine("guard-tails"),
                ProtectedAccessContextFactory.empty(),
            )

        fun guards(): Int = (0 until 100).count { npcs[it] != null }

        fun tick(): TailResult {
            val result = tails.tick(player)
            val npc = tails.guardNpc(player) ?: return result
            npc.routeDestination.lastOrNull()?.let { npc.coords = it }
            npc.routeDestination.clear()
            return result
        }
    }

    @Test fun `the guard walks through a player standing in his way`() {
        val rt = Runtime()
        rt.tails.start(rt.access)
        assertEquals(MoveRestrict.PassThru, checkNotNull(rt.tails.guardNpc(rt.player)).moveRestrict)
        rt.tails.stop(rt.player)
    }

    @Test fun `the runtime waits for the guard npc before moving the tail on`() {
        val rt = Runtime()
        rt.tails.start(rt.access)
        rt.player.coords = HideSpots[0]
        repeat(GuardTail.SetupTicks + 10) { rt.tails.tick(rt.player) }
        assertEquals(route[1], rt.tails.guardTile(rt.player))
        assertEquals(route[0], rt.tails.guardNpc(rt.player)?.coords)
        assertEquals(route[1], rt.tails.guardNpc(rt.player)?.routeDestination?.lastOrNull())
        rt.tails.stop(rt.player)
    }

    @Test fun `the runtime keeps one guard per player and removes it when the tail ends`() {
        val rt = Runtime()
        rt.tails.start(rt.access)
        rt.tails.start(rt.access)
        assertEquals(1, rt.guards())
        assertTrue(rt.player.timerMap.isNotEmpty)
        rt.tails.stop(rt.player)
        assertEquals(0, rt.guards())
        assertFalse(rt.player.timerMap.isNotEmpty)
        rt.tails.stop(rt.player)
    }

    @Test fun `the runtime follows the same rules as the tracker`() {
        val rt = Runtime()
        rt.tails.start(rt.access)
        var result: TailResult
        do {
            val index = route.indexOf(checkNotNull(rt.tails.guardTile(rt.player)))
            val passed = lookouts.count { it < index }
            rt.player.coords = HideSpots[passed.coerceAtMost(HideSpots.lastIndex)]
            result = rt.tick()
        } while (result == TailResult.Continue)
        assertEquals(TailResult.Arrived, result)
        assertEquals(route.last(), rt.tails.guardNpc(rt.player)?.coords)
        rt.tails.stop(rt.player)
    }

    @Test fun `the runtime reports a player who stays in the open as spotted`() {
        val rt = Runtime()
        rt.tails.start(rt.access)
        rt.player.coords = ExposedSpots[0]
        var result: TailResult
        do {
            result = rt.tick()
        } while (result == TailResult.Continue)
        assertEquals(TailResult.Spotted, result)
        assertFalse(rt.player.timerMap.isNotEmpty)
        assertEquals(1, rt.guards())
        rt.tails.stop(rt.player)
        assertEquals(0, rt.guards())
    }

    @Test fun `the runtime fails a player that has no guard`() {
        val rt = Runtime()
        assertEquals(TailResult.TooFar, rt.tails.tick(rt.player))
    }

    private companion object {
        val HideSpots =
            listOf(
                CoordGrid(3233, 3427),
                CoordGrid(3240, 3417),
                CoordGrid(3241, 3403),
                CoordGrid(3236, 3392),
                CoordGrid(3247, 3397),
            )

        val ExposedSpots =
            listOf(
                CoordGrid(3232, 3429),
                CoordGrid(3242, 3412),
                CoordGrid(3236, 3396),
                CoordGrid(3240, 3390),
                CoordGrid(3250, 3396),
            )

        lateinit var collision: CollisionFlagMap

        @JvmStatic
        @BeforeAll
        fun cache() {
            val cache = ServerCacheManager.init(240)
            try {
                collision = CollisionFlagMap()
                for (mx in 49..51) for (mz in 52..54) {
                    val group = (mx shl 8) or mz
                    val tilesData = cache.data(MAPS, group, 0) ?: continue
                    val locsData = cache.data(MAPS, group, 1) ?: continue
                    val tiles = MapTileDecoder.decode(InlineByteBuf(tilesData))
                    val spawns = MapLocListDecoder.decode(InlineByteBuf(locsData))
                    for (level in 0..3) for (x in 0 until 64 step 8) for (z in 0 until 64 step 8) {
                        collision.allocateIfAbsent(mx * 64 + x, mz * 64 + z, level)
                    }
                    val square = MapSquareKey(mx, mz)
                    GameMapDecoder.putMaps(collision, square, tiles)
                    GameMapDecoder.putLocs(GameMapBuilder(), collision, square, tiles, spawns)
                }
            } finally {
                cache.close()
            }
        }

        @JvmStatic
        @AfterAll
        fun release() {
            collision = CollisionFlagMap()
        }
    }
}
