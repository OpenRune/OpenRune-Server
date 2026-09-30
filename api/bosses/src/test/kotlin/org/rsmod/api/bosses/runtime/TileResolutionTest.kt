package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.BossStats
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.PhaseSpec
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class TileResolutionTest {
    private val spawn = CoordGrid(3200, 3200)
    private val npc = Npc(NpcServerType(id = 1, name = "Boss", size = 3, hitpoints = 100), spawn)
    private val player = Player().apply { coords = CoordGrid(3210, 3200) }
    private val random = DefaultGameRandom(Random(0))

    @Test
    fun `caster and target expressions resolve to their tiles`() {
        assertEquals(spawn, npc.resolveTile(TargetExpr.Self, player))
        assertEquals(spawn.translate(1, 1), npc.resolveTile(TargetExpr.Centre, player))
        assertEquals(spawn.translate(2, -1), npc.resolveTile(TargetExpr.SpawnTile(2, -1), player))
        assertEquals(player.coords, npc.resolveTile(TargetExpr.CurrentTarget, player))
        assertEquals(player.coords, npc.resolveTile(TargetExpr.LowestPrayer, player))
    }

    @Test
    fun `offset shifts its anchor and custom tiles run their function`() {
        val offsetTarget = TargetExpr.Offset(TargetExpr.CurrentTarget, -4, 4)
        assertEquals(player.coords.translate(-4, 4), npc.resolveTile(offsetTarget, player))
        val midway = TargetExpr.Custom { n, t -> CoordGrid((n.coords.x + t.coords.x) / 2, n.coords.z) }
        assertEquals(CoordGrid(3205, 3200), npc.resolveTile(midway, player))
        val current = CoordGrid(3206, 3206)
        val offsetCurrent = TargetExpr.Offset(TargetExpr.CurrentTile, 1, 0)
        val bindings = TileBindings(currentTile = current)
        assertEquals(current.translate(1, 0), npc.resolveTile(offsetCurrent, player, bindings))
    }

    @Test
    fun `bound tiles resolve only when bound`() {
        val impact = CoordGrid(3205, 3205)
        val current = CoordGrid(3206, 3206)
        val bindings = TileBindings(impactTile = impact, currentTile = current)
        assertEquals(impact, npc.resolveTile(TargetExpr.ImpactTile, player, bindings))
        assertEquals(current, npc.resolveTile(TargetExpr.CurrentTile, player, bindings))
        val impactOnly = TileBindings(impactTile = impact)
        assertThrows<IllegalStateException> { npc.resolveTile(TargetExpr.CurrentTile, player, impactOnly) }
    }

    @Test
    fun `named tiles and sets resolve from their bindings`() {
        val split = CoordGrid(3207, 3201)
        val debris = listOf(CoordGrid(3201, 3201), CoordGrid(3202, 3202))
        val bindings = TileBindings(tiles = mapOf("split" to split), sets = mapOf("debris" to debris))
        assertEquals(split, npc.resolveTile(TargetExpr.Bound("split"), player, bindings))
        assertEquals(split.translate(0, 1), npc.resolveTile(TargetExpr.Offset(TargetExpr.Bound("split"), 0, 1), player, bindings))
        repeat(10) {
            assertTrue(npc.resolveTile(TargetExpr.RandomOfBound("debris"), player, bindings, random) in debris)
        }
        assertThrows<IllegalStateException> { npc.resolveTile(TargetExpr.Bound("other"), player, bindings) }
    }

    @Test
    fun `randomOf an empty set throws`() {
        val bindings = TileBindings(sets = mapOf("debris" to emptyList()))
        val error =
            assertThrows<IllegalStateException> {
                npc.resolveTile(TargetExpr.RandomOfBound("debris"), player, bindings, random)
            }
        assertTrue("tilesEmpty" in error.message.orEmpty())
    }

    @Test
    fun `random walkable tile without a picker resolves to its centre`() {
        val expr = TargetExpr.RandomWalkableTile(radius = 3, of = TargetExpr.CurrentTarget)
        assertEquals(player.coords, npc.resolveTile(expr, player))
        assertEquals(spawn, npc.resolveTile(expr, player, randomWalkable = { _, _ -> spawn }))
    }

    @Test
    fun `TargetWithin resolves its anchor through the shared resolver`() {
        val encounter = encounter()
        assertTrue(encounter.evaluate(Condition.TargetWithin(0, TargetExpr.CurrentTarget), player))
        assertFalse(encounter.evaluate(Condition.TargetWithin(5, TargetExpr.SpawnTile()), player))
        assertTrue(encounter.evaluate(Condition.TargetWithin(10, TargetExpr.SpawnTile()), player))
    }

    @Test
    fun `conditions use the caller's bound tiles and sets`() {
        val encounter = encounter()
        val bindings =
            TileBindings(currentTile = player.coords, sets = mapOf("full" to listOf(spawn), "none" to emptyList()))
        val scope = scope(bindings)
        val within = Condition.TargetWithin(0, TargetExpr.CurrentTile)
        assertTrue(encounter.evaluate(within, player, scope))
        assertThrows<IllegalStateException> { encounter.evaluate(within, player) }
        assertFalse(encounter.evaluate(Condition.TilesEmpty("full"), player, scope))
        assertTrue(encounter.evaluate(Condition.TilesEmpty("none"), player, scope))
        assertThrows<IllegalStateException> { encounter.evaluate(Condition.TilesEmpty("none"), player) }
    }

    private fun scope(bindings: TileBindings): TileScope =
        object : TileScope {
            override fun tile(expr: TargetExpr.Single): CoordGrid = npc.resolveTile(expr, player, bindings, random)

            override fun set(name: String): List<CoordGrid> = bindings.set(name)
        }

    private fun encounter(): BossEncounter =
        BossEncounter(
            npc,
            BossSpec(
                npcTypes = listOf("npc.boss"),
                stats = BossStats(),
                abilities = mapOf("a" to resetAnim()),
                phases = mapOf("main" to PhaseSpec("main")),
                triggers = emptyList(),
            ),
            MapClock(),
        )
}
