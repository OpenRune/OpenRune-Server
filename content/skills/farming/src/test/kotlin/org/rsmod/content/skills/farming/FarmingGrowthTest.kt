package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.random.GameRandom

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingGrowthTest {
    @Test
    fun `crops resolve by their persisted index`() {
        assertEquals(FarmingCrops.all.sortedBy(Crop::index), FarmingCrops.all)
        for (crop in FarmingCrops.all) assertSame(crop, FarmingCrops.byIndex(crop.index))
        assertEquals(null, FarmingCrops.byIndex(0))
        assertEquals(null, FarmingCrops.byIndex(127))
    }

    @Test
    fun `a 20 minute crop advances exactly on its offset boundaries`() {
        val crop = crop("obj.guam_seed")
        val patch = patchFor(crop)
        val cycle = crop.cycleMinutes.toLong()
        for (offset in listOf(0, 7, 19)) {
            val boundary = cycle * 10 - offset
            val planted = planted(crop)
            assertEquals(planted, replay(patch, planted, boundary - cycle, boundary - 1, offset))
            assertEquals(1, replay(patch, planted, boundary - 1, boundary, offset).stage)
            assertEquals(1, replay(patch, planted, boundary - 1, boundary + cycle - 1, offset).stage)
            assertEquals(2, replay(patch, planted, boundary - 1, boundary + cycle, offset).stage)
            assertEquals(planted, replay(patch, planted, boundary, boundary + cycle - 1, offset))
        }
    }

    @Test
    fun `offline replay reaches maturity once and stays there`() {
        val crop = crop("obj.guam_seed")
        val grown = replay(patchFor(crop), planted(crop), 0, crop.cycleMinutes.toLong() * (crop.stages + 50), 0)
        assertEquals(crop.stages, grown.stage)
        assertEquals(Health.HEALTHY, grown.health)
    }

    @Test
    fun `a diseased herb dies only on the next 40 minute boundary`() {
        val guam = crop("obj.guam_seed")
        val patch = patchFor(guam)
        val diseased = planted(guam).copy(stage = 2, health = Health.DISEASED)
        val offset = 3
        val cycle = 40L * 5 + 20 - offset
        assertEquals(diseased, replay(patch, diseased, cycle - 1, cycle, offset))
        val death = 40L * 6 - offset
        val dead = replay(patch, diseased, death - 1, death, offset)
        assertEquals(diseased.copy(health = Health.DEAD), dead)
        assertEquals(dead, replay(patch, dead, death, death + 40 * 4, offset))
    }

    @Test
    fun `legacy watering protection remains until the sourced reduction replaces the model`() {
        val potato = crop("obj.potato_seed")
        val watered = planted(potato).copy(stage = 1, watered = true)
        val minute = potato.cycleMinutes.toLong()
        val random = FixedRandom(roll = 0, double = 0.0)
        val next = replay(patchFor(potato), watered, minute - 1, minute, 0, random)
        assertEquals(watered.copy(stage = 2, watered = false), next)
        assertEquals(0, random.rolls)
        val diseased = replay(patchFor(potato), next, minute, minute * 2, 0, random)
        assertEquals(next.copy(health = Health.DISEASED), diseased)
    }

    @Test
    fun `disease replaces the stage advance and never strikes the first stage`() {
        val guam = crop("obj.guam_seed")
        val patch = patchFor(guam)
        val always = FixedRandom(roll = 0)
        val seedling = planted(guam)
        val first = guam.cycleMinutes.toLong()
        assertEquals(1, replay(patch, seedling, first - 1, first, 0, always).stage)
        val growing = seedling.copy(stage = 1, watered = true)
        assertEquals(growing.copy(health = Health.DISEASED, watered = false), replay(patch, growing, first - 1, first, 0, always))
        val never = FixedRandom(roll = 127)
        assertEquals(growing.copy(stage = 2, watered = false), replay(patch, growing, first - 1, first, 0, never))
    }

    @Test
    fun `disease chance is floor of max over compost divisor plus one in 128`() {
        val guam = crop("obj.guam_seed")
        val patch = patchFor(guam)
        val minute = guam.cycleMinutes.toLong()
        for ((compost, highest) in listOf(Compost.NONE to 26, Compost.NORMAL to 13, Compost.SUPER to 5, Compost.ULTRA to 2)) {
            val growing = planted(guam).copy(stage = 1, compost = compost)
            assertEquals(Health.DISEASED, replay(patch, growing, minute - 1, minute, 0, FixedRandom(highest)).health)
            assertEquals(Health.HEALTHY, replay(patch, growing, minute - 1, minute, 0, FixedRandom(highest + 1)).health)
        }
    }

    @Test
    fun `disease-free patches skip the roll`() {
        val guam = crop("obj.guam_seed")
        val patch = patchFor(guam)
        val minute = guam.cycleMinutes.toLong()
        val growing = planted(guam).copy(stage = 1)
        val random = FixedRandom(roll = 0)
        val free = arrayOf(growing)
        FarmingGrowth(listOf(patch), booleanArrayOf(true), random).replay(free, minute - 1, minute, 0)
        assertEquals(growing.copy(stage = 2), free[0])
        assertEquals(0, random.rolls)
    }

    @Test
    fun `one grown flower protects every allotment on its site and no other`() {
        val site = FarmingPatches.all.first { flowerSite(it.site) }.site
        val allotments = FarmingPatches.bySite(site).filter { it.kind == PatchKind.ALLOTMENT }
        val flower = FarmingPatches.bySite(site).first { it.kind == PatchKind.FLOWER }
        val elsewhere = FarmingPatches.all.first { it.kind == PatchKind.ALLOTMENT && it.site != site }
        val potato = crop("obj.potato_seed")
        val marigold = checkNotNull(FarmingCrops.byIndex(potato.protectionIndex))
        val lily = crop("obj.white_lily_seed")
        val woad = crop("obj.woad_seed")
        val patches = allotments + flower + elsewhere
        val growing = planted(potato).copy(stage = 1)
        val cases =
            listOf(
                grown(marigold) to true,
                grown(lily) to true,
                grown(woad) to false,
                planted(marigold).copy(stage = 1) to false,
                grown(marigold).copy(health = Health.DISEASED) to false,
                grown(marigold).copy(health = Health.DEAD) to false,
            )
        val minute = potato.cycleMinutes.toLong()
        for ((protector, protects) in cases) {
            val states = Array(patches.size) { if (patches[it] == flower) protector else growing }
            FarmingGrowth(patches, BooleanArray(patches.size), FixedRandom(roll = 0, double = 0.0))
                .replay(states, minute - 1, minute, 0)
            for (i in allotments.indices) {
                assertEquals(if (protects) Health.HEALTHY else Health.DISEASED, states[i].health, "$protector")
            }
            assertEquals(Health.DISEASED, states.last().health)
        }
    }

    @Test
    fun `protection starts the tick after the flower matures`() {
        val site = FarmingPatches.all.first { flowerSite(it.site) }.site
        val allotment = FarmingPatches.bySite(site).first { it.kind == PatchKind.ALLOTMENT }
        val flower = FarmingPatches.bySite(site).first { it.kind == PatchKind.FLOWER }
        val potato = crop("obj.potato_seed")
        val marigold = checkNotNull(FarmingCrops.byIndex(potato.protectionIndex))
        assertEquals(0, potato.cycleMinutes % marigold.cycleMinutes)
        val maturing = planted(marigold).copy(stage = marigold.stages - 1)
        val states = arrayOf(planted(potato).copy(stage = 1), maturing)
        val minute = potato.cycleMinutes.toLong()
        FarmingGrowth(listOf(allotment, flower), BooleanArray(2) { it == 1 }, FixedRandom(roll = 0, double = 0.0))
            .replay(states, minute - 1, minute, 0)
        assertEquals(Health.DISEASED, states[0].health)
        assertEquals(marigold.stages, states[1].stage)
    }

    @Test
    fun `empty patches regrow one weed stage on each five minute tick`() {
        val patch = FarmingPatches.all.first()
        val raked = PatchState(weeds = PatchState.CLEARED)
        assertEquals(raked, replay(patch, raked, 1, 4, 0))
        assertEquals(raked.copy(weeds = 2), replay(patch, raked, 4, 5, 0))
        assertEquals(raked.copy(weeds = 2), replay(patch, raked, 0, 4, 1))
        assertEquals(PatchState(), replay(patch, raked, 0, 15, 0))
        assertEquals(PatchState(), replay(patch, raked, 0, 525_600, 0))
    }

    @Test
    fun `a year offline with only settled patches finishes without rolling`() {
        val potato = crop("obj.potato_seed")
        val random = FixedRandom(roll = 0, double = 0.0)
        val states = arrayOf(grown(potato), grown(potato).copy(health = Health.DEAD), PatchState())
        val patches = FarmingPatches.all.filter { it.kind == PatchKind.ALLOTMENT }.take(3)
        FarmingGrowth(patches, BooleanArray(3), random).replay(states, 0, 525_600L * 1000, 0)
        assertEquals(0, random.rolls)
        assertTrue(states.contentEquals(arrayOf(grown(potato), grown(potato).copy(health = Health.DEAD), PatchState())))
    }

    private class FixedRandom(private val roll: Int, private val double: Double = 0.999) : GameRandom {
        var rolls = 0

        override fun of(maxExclusive: Int): Int = roll.also { rolls++ }

        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive

        override fun randomDouble(): Double = double.also { rolls++ }
    }

    private companion object {
        fun crop(seed: String): Crop = checkNotNull(FarmingCrops.bySeed(seed)) { seed }

        fun patchFor(crop: Crop): PatchDef = FarmingPatches.all.first { it.kind == crop.kind }

        fun flowerSite(site: String): Boolean =
            FarmingPatches.bySite(site).count { it.kind == PatchKind.ALLOTMENT } == 2 &&
                FarmingPatches.bySite(site).any { it.kind == PatchKind.FLOWER }

        fun planted(crop: Crop) = PatchState(weeds = PatchState.CLEARED, cropIndex = crop.index)

        fun grown(crop: Crop) = planted(crop).copy(stage = crop.stages).matured(crop)

        fun replay(
            patch: PatchDef,
            state: PatchState,
            from: Long,
            to: Long,
            offset: Int,
            random: GameRandom = FixedRandom(roll = 127),
        ): PatchState {
            val states = arrayOf(state)
            FarmingGrowth(listOf(patch), BooleanArray(1), random).replay(states, from, to, offset)
            return states[0]
        }

        @JvmStatic
        @BeforeAll
        fun cache() {
            loadFarmingTestCache()
        }
    }
}
