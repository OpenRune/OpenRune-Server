package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingTreeStumpsTest {
    @Test
    fun `root count is one at the crop level and one more per cache tier reached`() {
        val f = Fixture()
        for ((level, count) in listOf(45 to 1, 52 to 1, 53 to 2, 60 to 2, 61 to 3, 68 to 3, 69 to 4, 99 to 4)) {
            f.stump()
            assertEquals(StumpDig.DONE, f.finish(level), "level $level")
            assertEquals(count, f.roots(), "level $level")
            f.clearInv()
        }
    }

    @Test
    fun `a full inventory keeps the stump and grants nothing`() {
        val f = Fixture()
        f.fillWith("obj.spade")
        val stump = f.stump()
        assertEquals(StumpDig.NO_ROOM, f.finish(99))
        assertEquals(0, f.roots())
        assertEquals(stump, f.state())
        assertTrue(f.cleared.isEmpty())
    }

    @Test
    fun `too few free slots for every root grants none and a fitting count grants all`() {
        val f = Fixture()
        f.fillWith("obj.spade", free = 3)
        val stump = f.stump()
        assertEquals(StumpDig.NO_ROOM, f.finish(99))
        assertEquals(0, f.roots())
        assertEquals(stump, f.state())
        f.player.inv[0] = null
        assertEquals(StumpDig.DONE, f.finish(99))
        assertEquals(4, f.roots())
        assertEquals(1, f.cleared.size)
        assertFalse(f.cleared.single().dead)
    }

    @Test
    fun `a regrown stump grants nothing`() {
        val f = Fixture()
        val stump = f.stump()
        f.regrow()
        assertEquals(Phase.CHECKED, f.state().phase)
        assertEquals(StumpDig.REGROWN, f.finish(99, stump))
        assertEquals(0, f.roots())
        assertTrue(f.cleared.isEmpty())
    }

    @Test
    fun `digging an already emptied patch grants nothing`() {
        val f = Fixture()
        val stump = f.stump()
        assertEquals(StumpDig.DONE, f.finish(99, stump))
        assertEquals(4, f.roots())
        assertEquals(StumpDig.CHANGED, f.finish(99, stump))
        assertEquals(4, f.roots())
        assertEquals(1, f.cleared.size)
    }

    @Test
    fun `deplete stores the stump and regrowth restores the checked tree`() {
        val f = Fixture()
        val checked = f.stump().copy(phase = Phase.CHECKED)
        f.set(checked)
        f.stumps.deplete(f.player, f.patch, checked)
        assertEquals(Phase.STUMP, f.state().phase)
        f.regrow()
        assertEquals(checked, f.state())
    }

    @Test
    fun `the regrow timer turns a stump back into its checked tree`() {
        val f = Fixture()
        f.stump()
        f.stumps.regrowAll(f.player)
        assertEquals(Phase.CHECKED, f.state().phase)
    }

    private class Fixture {
        private val events = EventBus()
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int): Int = maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int): Int = maxInclusive
            override fun randomDouble(): Double = 0.999
        }
        private val farming = FarmingService(
            XpModifiers(emptySet()), random, events,
            FarmingRewardTestSupport.rewards(random, events), FarmingEquipment(random),
            farmingSiteAccess(),
        )
        val stumps = FarmingTreeStumps(farming)
        val patch = FarmingPatches.all.first { it.kind == PatchKind.TREE }
        private val crop = checkNotNull(FarmingCrops.bySeed("obj.plantpot_maple_sapling"))
        val cleared = mutableListOf<FarmingClearEvent>()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        }

        init {
            VarPlayerIntMapSetter.set(player, "varp.farming_clock", Int.MAX_VALUE)
            events.subscribeUnbound(FarmingClearEvent::class.java) { cleared += this }
        }

        fun set(state: PatchState) = VarPlayerIntMapSetter.set(player, patch.varp, state.pack())
        fun state(): PatchState = PatchState.unpack(player.vars[patch.varp])
        fun stump(): PatchState =
            PatchState(weeds = 3, cropIndex = crop.index, stage = crop.stages, phase = Phase.STUMP)
                .also(::set)
        fun regrow() = stumps.regrow(player, patch)
        fun finish(level: Int, expected: PatchState = state()) =
            farming.finishStumpDig(player, patch, expected, level)
        fun roots(): Int = player.inv.physicalCount("obj.maple_roots")
        fun clearInv() = player.inv.indices.forEach { player.inv[it] = null }
        fun fillWith(obj: String, free: Int = 0) {
            for (slot in 0 until player.inv.size - free) player.inv[slot] = InvObj(obj)
        }
    }

    companion object {
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
