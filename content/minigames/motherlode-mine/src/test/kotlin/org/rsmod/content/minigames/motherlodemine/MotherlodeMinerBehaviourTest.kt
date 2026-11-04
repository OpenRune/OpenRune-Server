package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.npc.NpcMovementProcessor
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.route.StepFactory
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class MotherlodeMinerBehaviourTest {
    @Test fun `miners leave distant rockfalls alone even when their clearing roll succeeds`() {
        val f = Fixture(clearRockfalls = true)
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START.translate(3, 0))
        val vein = f.loc(MotherlodeConstants.VEIN_SINGLE, START.translate(-3, 0))
        val npc = f.miner().apply { coords = START.translate(-4, 0) }
        repeat(200) { tick(f, npc) }
        assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
        assertTrue(f.info(npc).sequences.any { it > 0 })
        assertEquals(vein.coords, npc.pendingFaceSquare)
        assertEquals(START.translate(-4, 0), npc.coords)
    }

    @Test fun `miners keep working a vein instead of clearing every rockfall respawn`() {
        val f = Fixture(clearRockfalls = true)
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1)
        val vein = f.loc(MotherlodeConstants.VEIN_SINGLE, START.translate(-1, 0))
        val npc = f.miner()
        repeat(3) { tick(f, npc) }
        assertNull(f.locs.findExact(rockfall.coords, rockfall.shape))
        repeat(MotherlodeConstants.ROCKFALL_RESPAWN_TICKS + 20) { tick(f, npc) }
        assertEquals(rockfall.id, f.locs.findExact(rockfall.coords, rockfall.shape)?.id)
        assertEquals(vein.id, f.locs.findExact(vein.coords, vein.shape)?.id)
        assertEquals(vein.coords, npc.pendingFaceSquare)
    }

    @Test fun `miners can clear a rockfall three tiles away from a two-step approach`() {
        val f = Fixture(clearRockfalls = true)
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START.translate(3, 0))
        val npc = f.miner()
        repeat(6) {
            tick(f, npc)
            assertTrue(npc.coords.chebyshevDistance(START) <= 2)
        }
        assertEquals(START.translate(2, 0), npc.coords)
        assertNull(f.locs.findExact(rockfall.coords, rockfall.shape))
        assertTrue(f.info(npc).sequences.any { it > 0 })
    }

    @Test fun `mining choices work periods and pauses vary without leaving the local area`() {
        val f = Fixture(randomSource = DefaultGameRandom(seed = 42))
        val veins = listOf(
            f.loc(MotherlodeConstants.VEIN_SINGLE, START.translate(-4, 0)),
            f.loc(MotherlodeConstants.VEIN_SINGLE, START.translate(4, 0)),
        )
        val npc = f.miner()
        val mined = mutableSetOf<CoordGrid>()
        val workPeriods = mutableSetOf<Int>()
        val rests = mutableSetOf<Int>()
        var miningSince: Int? = null
        var restedSince: Int? = null
        repeat(600) {
            tick(f, npc)
            assertTrue(npc.coords.chebyshevDistance(npc.spawnCoords) <= 4)
            val active = (f.info(npc).sequences.lastOrNull() ?: -1) > 0
            if (active) {
                mined += npc.pendingFaceSquare
                if (miningSince == null) {
                    miningSince = f.clock.cycle
                    restedSince?.let { start -> rests += f.clock.cycle - start }
                    restedSince = null
                }
            } else if (miningSince != null) {
                workPeriods += f.clock.cycle - checkNotNull(miningSince)
                miningSince = null
                restedSince = f.clock.cycle
            }
        }
        assertEquals(veins.map { it.coords }.toSet(), mined)
        assertTrue(workPeriods.size > 1, "Mining sessions should last different lengths")
        assertTrue(rests.size > 1, "Miners should vary their pauses between jobs")
        for (vein in veins) assertEquals(vein.id, f.locs.findExact(vein.coords, vein.shape)?.id)
        assertTrue(f.info(npc).messages.size <= 6)
    }

    private fun tick(f: Fixture, npc: Npc) {
        npc.previousCoords = npc.coords
        npc.currentMapClock = f.clock.cycle
        npc.processedMapClock = f.clock.cycle
        f.rockfalls.tick()
        f.miners.mine(npc)
        NpcMovementProcessor(f.collision, StepFactory(f.collision), f.events).process(npc)
        f.clock.tick()
    }

    companion object {
        private val START = CoordGrid(3755, 5680)

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
